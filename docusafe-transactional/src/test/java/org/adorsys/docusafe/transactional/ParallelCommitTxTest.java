package org.adorsys.docusafe.transactional;

import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Created by peter on 13.06.18 at 11:38.
 */
// @SuppressWarnings("Duplicates")
public class ParallelCommitTxTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(ParallelCommitTxTest.class);
    private final static int PARALLEL_INSTANCES = 5;
    private final static String FILENAME = "paralleltest.txt";


    @Test
    public void parallelCommits() {
        ThreadMemoryContextImpl requestMemoryContext = new ThreadMemoryContextImpl();
        DocumentSafeServiceImpl dssi = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
        TransactionalDocumentSafeService transactionalFileStorage = new TransactionalDocumentSafeServiceImpl(requestMemoryContext, dssi);

        try {
            Semaphore semaphore = new Semaphore(PARALLEL_INSTANCES);
            CountDownLatch countDownLatch = new CountDownLatch(PARALLEL_INSTANCES);
            semaphore.acquire(PARALLEL_INSTANCES);
            ARunnable[] runnables = new ARunnable[PARALLEL_INSTANCES];
            Thread[] instances = new Thread[PARALLEL_INSTANCES];
            for (int i = 0; i < PARALLEL_INSTANCES; i++) {
                runnables[i] = new ARunnable(semaphore, countDownLatch, transactionalFileStorage, userIDAuth);
                instances[i] = new Thread(runnables[i]);
                instances[i].start();
            }
            Thread.currentThread().sleep(2000);
            // Lege erste Version von first.txt an
            {
                transactionalFileStorage.createUser(userIDAuth);

                DocumentFQN documentFQN = new DocumentFQN(FILENAME);
                DocumentContent documentContent = new DocumentContent("very first".getBytes());
                DSDocument document = new DSDocument(documentFQN, documentContent);

                transactionalFileStorage.beginTransaction(userIDAuth);
                LOGGER.debug("FIRST TXID");
                Assert.assertFalse(transactionalFileStorage.txDocumentExists(userIDAuth, documentFQN));
                transactionalFileStorage.txStoreDocument(userIDAuth, document);
                Assert.assertTrue(transactionalFileStorage.txDocumentExists(userIDAuth, documentFQN));
                transactionalFileStorage.endTransaction(userIDAuth);
            }

            LOGGER.debug("start " + PARALLEL_INSTANCES + " threads concurrently now");
            semaphore.release(PARALLEL_INSTANCES);
            LOGGER.debug("wait for " + PARALLEL_INSTANCES + " to finsih");
            countDownLatch.await();
            LOGGER.debug(PARALLEL_INSTANCES + " threadas have finished");

            Set<Integer> winner = new HashSet<>();
            for (int i = 0; i < PARALLEL_INSTANCES; i++) {
                winner.add(i);
            }
            int errorCounter = 0;
            for (int i = 0; i < PARALLEL_INSTANCES; i++) {
                if (!runnables[i].ok) {
                    errorCounter++;
                    LOGGER.error("THREAD " + runnables[i].instanceID + " error " + errorCounter + " " + runnables[i].exception.getMessage());
                    winner.remove(runnables[i].instanceID);
                }
            }
            // only one tx can be closed, the others are too late
            Assert.assertEquals(PARALLEL_INSTANCES - 1, errorCounter);
            Assert.assertEquals(1, winner.size());
            Integer[] winnerInts = new Integer[winner.size()];
            winner.toArray(winnerInts);
            Integer winnerInstanceID = winnerInts[0];
            LOGGER.info("=================================================================================================");
            LOGGER.info("the winner of the " + PARALLEL_INSTANCES + " instances is thread number " + winnerInstanceID);
            transactionalFileStorage.beginTransaction(userIDAuth);
            DSDocument dsDocument = transactionalFileStorage.txReadDocument(userIDAuth, new DocumentFQN(FILENAME));
            LOGGER.info("Content found is " + new String(dsDocument.getDocumentContent().getValue()));
            DocumentContent expectedContent = new DocumentContent(("Thread Number " + winnerInstanceID).getBytes());
            LOGGER.info("expected Content  is " + new String(expectedContent.getValue()));
            Assert.assertEquals(expectedContent, dsDocument.getDocumentContent());
            transactionalFileStorage.endTransaction(userIDAuth);

        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public static class ARunnable implements Runnable {
        private final static Logger LOGGER = LoggerFactory.getLogger(ARunnable.class);
        private static int instanceCounter = 0;

        private int instanceID;
        private Semaphore sem;
        private TransactionalDocumentSafeService transactionalFileStorage;
        private UserIDAuth userIDAuth;
        private CountDownLatch countDownLatch;
        public boolean ok = false;
        public Exception exception;

        public ARunnable(Semaphore sem, CountDownLatch countDownLatch, TransactionalDocumentSafeService transactionalFileStorage, UserIDAuth userIDAuth) {
            this.instanceID = instanceCounter++;
            this.sem = sem;
            this.transactionalFileStorage = transactionalFileStorage;
            this.userIDAuth = userIDAuth;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                DocumentFQN documentFQN = new DocumentFQN(FILENAME);
                DocumentContent documentContent = new DocumentContent(("Thread Number " + instanceID).getBytes());
                DSDocument document = new DSDocument(documentFQN, documentContent);

                sem.acquire();

                transactionalFileStorage.beginTransaction(userIDAuth);
                transactionalFileStorage.txStoreDocument(userIDAuth, document);
                transactionalFileStorage.endTransaction(userIDAuth);

                sem.release();
                ok = true;
                LOGGER.info("Thread " + instanceID + " successfully wrote file " + FILENAME + " with content \"" + new String(documentContent.getValue()) + "\"");
            } catch (Exception e) {
                exception = e;
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}

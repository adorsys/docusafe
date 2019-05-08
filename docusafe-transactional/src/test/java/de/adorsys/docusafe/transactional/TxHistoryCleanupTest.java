package de.adorsys.docusafe.transactional;

import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import de.adorsys.docusafe.business.types.BucketContentFQN;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import de.adorsys.docusafe.transactional.impl.TxIDLog;
import de.adorsys.docusafe.transactional.impl.helper.CleanupLogic;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by peter on 11.07.18 at 11:20.
 */
@Slf4j
@RunWith(value = PowerMockRunner.class)
@PrepareForTest({CleanupLogic.class})
@PowerMockIgnore("javax.*")
public class TxHistoryCleanupTest extends TransactionalDocumentSafeServiceBaseTest {

    /**
     * Here it is only tested, that the cleanup logic is called at all
     * No check how many files will be cleaned
     */
    @SneakyThrows
    @Test
    public void createFilesAndDeleteSomeRandomFilesInServeralTransactions() {
        CleanupLogic cl = Mockito.spy(new CleanupLogic());

        PowerMockito.whenNew(CleanupLogic.class).withNoArguments().thenAnswer(in -> {
            log.info("powermodckit works fine for CleanupLogic");
            return cl;
        });

        StopWatch st = new StopWatch();
        st.start();

        Map<DocumentFQN, DocumentContent> memoryMap = new HashMap<>();

        int numberOfTransactinos = 3;
        int numberOfFilesToDeletePerTx = 1;
        int numberOfFilesToCreatePerTx = 3;
        int numberOfFilesToOverwritePerTx = 2;
        int expectedNumberOfFilesAfterIteration = (numberOfFilesToCreatePerTx * numberOfTransactinos) - (numberOfTransactinos * numberOfFilesToDeletePerTx);

        transactionalDocumentSafeService.createUser(userIDAuth);
        DocumentDirectoryFQN documentDirectoryFQN = new DocumentDirectoryFQN("folder");

        log.info("numberOfTransactions:                " + numberOfTransactinos);
        log.info("numberOfFilesToDeletePerTx:          " + numberOfFilesToDeletePerTx);
        log.info("numberOfFilesToCreatePerTx:          " + numberOfFilesToCreatePerTx);
        log.info("numberOfFilesToOverwritePerTx:       " + numberOfFilesToOverwritePerTx);
        log.info("expectedNumberOfFilesAfterIteration: " + expectedNumberOfFilesAfterIteration);

        int staticCounter = 0;
        {
            // create documents
            for (int i = 0; i < numberOfTransactinos; i++) {
                log.debug("create LIST OF FILES IN DOCUMENTSAFE: " + dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).size());
                transactionalDocumentSafeService.beginTransaction(userIDAuth);
                log.debug("create LIST OF FILES IN TX: " + transactionalDocumentSafeService.txListDocuments(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).getFilesWithVersion().size());
                for (int j = 0; j < numberOfFilesToCreatePerTx; j++) {
                    DSDocument document = new DSDocument(documentDirectoryFQN.addName("file_" + staticCounter++ + ".TXT"),
                            new DocumentContent(("Content of File " + i).getBytes()));
                    transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
                    memoryMap.put(document.getDocumentFQN(), document.getDocumentContent());
                }
                // show("create loop:", transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE));
                transactionalDocumentSafeService.endTransaction(userIDAuth);
            }
        }
        {
            // delete documentes
            for (int i = 0; i < numberOfTransactinos; i++) {
                log.debug("delete LIST OF FILES IN DOCUMENTSAFE: " + dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).size());
                transactionalDocumentSafeService.beginTransaction(userIDAuth);
                log.debug("delete LIST OF FILES IN TX: " + transactionalDocumentSafeService.txListDocuments(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).getFilesWithVersion().size());
                for (int j = 0; j < numberOfFilesToDeletePerTx; j++) {
                    BucketContentFQN bucketContentFQN = transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE);
                    int currentNumberOfFiles = bucketContentFQN.getFiles().size();
                    int indexToDelete = ThreadLocalRandom.current().nextInt(0, currentNumberOfFiles);
                    log.debug("Transaction number " + i + " has " + currentNumberOfFiles + " files");
                    log.debug("Index to delete is " + indexToDelete);
                    transactionalDocumentSafeService.txDeleteDocument(userIDAuth, bucketContentFQN.getFiles().get(indexToDelete));
                    memoryMap.remove(bucketContentFQN.getFiles().get(indexToDelete));

                }

                // show("delete loop:", transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE));
                transactionalDocumentSafeService.endTransaction(userIDAuth);
            }
        }
        {
            // overwrite documents
            for (int i = 0; i < numberOfTransactinos; i++) {
                log.debug("overwrite LIST OF FILES IN DOCUMENTSAFE: " + dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).size());
                transactionalDocumentSafeService.beginTransaction(userIDAuth);
                log.debug("overwrite LIST OF FILES IN TX: " + transactionalDocumentSafeService.txListDocuments(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).getFilesWithVersion().size());
                for (int j = 0; j < numberOfFilesToOverwritePerTx; j++) {
                    // show("overwrite loop", transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE));
                    BucketContentFQN bucketContentFQN = transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE);
                    int currentNumberOfFiles = bucketContentFQN.getFiles().size();
                    int indexToOverwrite = ThreadLocalRandom.current().nextInt(0, currentNumberOfFiles);
                    DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, bucketContentFQN.getFiles().get(indexToOverwrite));
                    DSDocument newDsDocument = new DSDocument(dsDocument.getDocumentFQN(),
                            new DocumentContent((new String(dsDocument.getDocumentContent().getValue()) + " overwritten in tx ").getBytes()));
                    transactionalDocumentSafeService.txStoreDocument(userIDAuth, newDsDocument);
                    memoryMap.put(newDsDocument.getDocumentFQN(), newDsDocument.getDocumentContent());
                }
                transactionalDocumentSafeService.endTransaction(userIDAuth);
            }
        }
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            BucketContentFQN bucketContentFQN = transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE);
            log.debug("LIST OF FILES IN TRANSACTIONAL LAYER: " + bucketContentFQN.toString());
            Assert.assertEquals(memoryMap.keySet().size(), bucketContentFQN.getFiles().size());
            bucketContentFQN.getFiles().forEach(documentFQN -> {
                DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
                Assert.assertArrayEquals(memoryMap.get(documentFQN).getValue(), dsDocument.getDocumentContent().getValue());
                log.debug(documentFQN + " checked!");
            });
            transactionalDocumentSafeService.endTransaction(userIDAuth);
            Assert.assertEquals(expectedNumberOfFilesAfterIteration, bucketContentFQN.getFiles().size());
        }

        // Nun gehen wir direkt auf das Filesystem. Hier gibt es nun alle Dateien zu sehen
        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        int finalNumberOfDocuments = transactionalDocumentSafeService.txListDocuments(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).getFilesWithVersion().size();
        log.debug("overwrite LIST OF FILES IN TX: " + finalNumberOfDocuments);
        transactionalDocumentSafeService.endTransaction(userIDAuth);
        log.debug("finally LIST OF FILES IN DOCUMENTSAFE: " + dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).size());
        dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).forEach(el -> log.debug(el.toString()));
        st.stop();
        Assert.assertEquals(expectedNumberOfFilesAfterIteration, finalNumberOfDocuments);
        log.debug("time for test " + st.toString());
        Mockito.verify(cl, Mockito.atLeast(1)).cleanupTxHistory(Mockito.any(), Mockito.any(), Mockito.any());

    }

    /**
     * This test forces the cleanup to run in parallal.
     * So one thread is getting errors during cleanup, the other not.
     */
    @SneakyThrows
    @Test
    public void forceCleanupInParallel() {
        int numberOfTx = TxIDLog.MAX_COMMITED_TX_FOR_CLEANUP + 1;
        transactionalDocumentSafeService.createUser(userIDAuth);
        List<Date> transactionStartTimes = new ArrayList<>();
        for (int i = 0; i < numberOfTx; i++) {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            transactionStartTimes.add(new Date());

            DSDocument document = new DSDocument(new DocumentFQN("folder/file.txt"), new DocumentContent(("Content of File").getBytes()));
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }

        dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).forEach(el -> log.debug(el.toString()));

        // untill now, the cleanup has not been called yet
        Assert.assertEquals(2 * numberOfTx + 1, dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE).size());

        // now we start two tx in parallel, to make sure, both call the cleanup, this should give conficts

        int PARALLEL_INSTANCES = 2;
        Semaphore semaphore = new Semaphore(PARALLEL_INSTANCES);
        CountDownLatch countDownLatch = new CountDownLatch(PARALLEL_INSTANCES);
        semaphore.acquire(PARALLEL_INSTANCES);
        ARunnable[] runnables = new ARunnable[PARALLEL_INSTANCES];
        Thread[] instances = new Thread[PARALLEL_INSTANCES];
        for (int i = 0; i < PARALLEL_INSTANCES; i++) {
            runnables[i] = new ARunnable(semaphore, countDownLatch, userIDAuth);
            instances[i] = new Thread(runnables[i]);
            instances[i].start();
        }

        Thread.currentThread().sleep(100);
        log.debug("start " + PARALLEL_INSTANCES + " threads concurrently now");
        semaphore.release(PARALLEL_INSTANCES);
        log.debug("wait for " + PARALLEL_INSTANCES + " to finsih");
        countDownLatch.await();
        log.debug(PARALLEL_INSTANCES + " threadas have finished");

        for (int i = 0; i < PARALLEL_INSTANCES; i++) {
            log.debug(runnables[i].instanceID + " -> " + runnables[i].ok);
        }

    }

    public static class ARunnable implements Runnable {
        private final static Logger LOGGER = LoggerFactory.getLogger(ParallelCommitTxTest.ARunnable.class);
        private static int instanceCounter = 0;

        private int instanceID;
        private Semaphore sem;
        private TransactionalDocumentSafeService transactionalFileStorage;
        private UserIDAuth userIDAuth;
        private CountDownLatch countDownLatch;
        public boolean ok = false;
        public Exception exception;

        public ARunnable(Semaphore sem, CountDownLatch countDownLatch, UserIDAuth userIDAuth) {
            this.instanceID = instanceCounter++;
            this.sem = sem;
            this.userIDAuth = userIDAuth;
            this.countDownLatch = countDownLatch;
            SimpleRequestMemoryContextImpl requestMemoryContext = new SimpleRequestMemoryContextImpl();
            DocumentSafeService dss = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
            this.transactionalFileStorage = new TransactionalDocumentSafeServiceImpl(requestMemoryContext, dss);

        }

        @Override
        public void run() {
            try {
                sem.acquire();
                LOGGER.info("Thread " + instanceID + " successfully started");
                transactionalFileStorage.beginTransaction(userIDAuth);
                sem.release();
                ok = true;
                LOGGER.info("Thread " + instanceID + " successfully started transaction");
            } catch (Exception e) {
                exception = e;
            } finally {
                countDownLatch.countDown();
            }
        }
    }

}

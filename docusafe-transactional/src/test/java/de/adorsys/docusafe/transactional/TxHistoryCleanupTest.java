package de.adorsys.docusafe.transactional;

import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.docusafe.business.types.BucketContentFQN;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.DocumentContent;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by peter on 11.07.18 at 11:20.
 */
@Slf4j
@RunWith(value = PowerMockRunner.class)
@PrepareForTest({CleanupLogic.class})
@PowerMockIgnore("javax.*")
public class TxHistoryCleanupTest extends TransactionalDocumentSafeServiceBaseTest {

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
                    int indexToDelete = getRandomInRange(currentNumberOfFiles);
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
                    int indexToOverwrite = getRandomInRange(currentNumberOfFiles);
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
        Mockito.verify(cl, Mockito.times(1)).cleanupTxHistory(Mockito.any(), Mockito.any(), Mockito.any());

    }

    private void show(String description, BucketContentFQN bucketContentFQN) {
        log.info("--------------------------------- begin " + description);
        log.info("files" + bucketContentFQN.getFiles().size());
        bucketContentFQN.getFiles().forEach(dir -> log.info(dir.toString()));
        log.info("--------------------------------- end " + description);
    }


    private int getRandomInRange(int max) {
        int random = ThreadLocalRandom.current().nextInt(0, max);
        // log.debug("ramdom in max " + max + " is " + random);
        return random;
    }
}

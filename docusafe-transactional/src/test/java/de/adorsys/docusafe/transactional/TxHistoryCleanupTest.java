package de.adorsys.docusafe.transactional;

import de.adorsys.docusafe.business.types.BucketContentFQN;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by peter on 11.07.18 at 11:20.
 */
public class TxHistoryCleanupTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(TxHistoryCleanupTest.class);

    @Test
    public void createFilesAndDeleteSomeRandomFilesInServeralTransactions() {
        

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

        LOGGER.info("numberOfTransactions:                "  + numberOfTransactinos);
        LOGGER.info("numberOfFilesToDeletePerTx:          " + numberOfFilesToDeletePerTx);
        LOGGER.info("numberOfFilesToCreatePerTx:          " + numberOfFilesToCreatePerTx);
        LOGGER.info("numberOfFilesToOverwritePerTx:       " + numberOfFilesToOverwritePerTx);
        LOGGER.info("expectedNumberOfFilesAfterIteration: " + expectedNumberOfFilesAfterIteration);

        int staticCounter = 0;
        {
            // create documents
            for (int i = 0; i < numberOfTransactinos; i++) {
                transactionalDocumentSafeService.beginTransaction(userIDAuth);
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
                transactionalDocumentSafeService.beginTransaction(userIDAuth);
                for (int j = 0; j < numberOfFilesToDeletePerTx; j++) {
                    BucketContentFQN bucketContentFQN = transactionalDocumentSafeService.txListDocuments(userIDAuth, documentDirectoryFQN, ListRecursiveFlag.TRUE);
                    int currentNumberOfFiles = bucketContentFQN.getFiles().size();
                    int indexToDelete = getRandomInRange(currentNumberOfFiles);
                    LOGGER.debug("Transaction number " + i + " has " + currentNumberOfFiles + " files");
                    LOGGER.debug("Index to delete is " + indexToDelete);
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
                transactionalDocumentSafeService.beginTransaction(userIDAuth);
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
            LOGGER.debug("LIST OF FILES IN TRANSACTIONAL LAYER: " + bucketContentFQN.toString());
            Assert.assertEquals(memoryMap.keySet().size(), bucketContentFQN.getFiles().size());
            bucketContentFQN.getFiles().forEach(documentFQN -> {
                DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
                Assert.assertArrayEquals(memoryMap.get(documentFQN).getValue(), dsDocument.getDocumentContent().getValue());
                LOGGER.debug(documentFQN + " checked!");
            });
            transactionalDocumentSafeService.endTransaction(userIDAuth);
            Assert.assertEquals(expectedNumberOfFilesAfterIteration, bucketContentFQN.getFiles().size());
        }

        // Nun gehen wir direkt auf das Filesystem. Hier gibt es nun alle Dateien zu sehen
        List<DocumentFQN> list = dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE);
        LOGGER.debug("LIST OF FILES IN DOCUMENTSAFE: " + list.toString());
//        Assert.assertEquals(numberOfFiles, list.getFiles().size());
        st.stop();
        LOGGER.debug("time for test " + st.toString());
    }

    private void show(String description, BucketContentFQN bucketContentFQN) {
        LOGGER.info("--------------------------------- begin " + description);
        LOGGER.info("files" + bucketContentFQN.getFiles().size());
        bucketContentFQN.getFiles().forEach(dir -> LOGGER.info(dir.toString()));
        LOGGER.info("--------------------------------- end " + description);
    }


    private int getRandomInRange(int max) {
        // nextInt is normally exclusive of the top value,
        LOGGER.debug("Also max ist " + max);
        return ThreadLocalRandom.current().nextInt(0, max);
    }
}

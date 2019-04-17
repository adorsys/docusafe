package org.adorsys.docusafe.transactional;

import com.googlecode.catchexception.CatchException;
import org.adorsys.docusafe.business.types.MoveType;
import org.adorsys.docusafe.business.types.BucketContentFQN;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.transactional.exceptions.TxInnerException;
import org.adorsys.docusafe.transactional.exceptions.TxNotActiveException;
import org.adorsys.docusafe.transactional.exceptions.TxRacingConditionException;
import org.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import org.adorsys.docusafe.transactional.types.TxDocumentFQNVersion;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by peter on 12.06.18 at 08:44.
 */
public class TransactionalDocumentSafeServiceTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionalDocumentSafeServiceTest.class);

    @Test
    public void sendDocumentFromSystemUserToPeter() {

        transactionalDocumentSafeService.createUser(userIDAuth);
        transactionalDocumentSafeService.createUser(systemUserIDAuth);

        DocumentFQN systemUserSourceDocFileName = new DocumentFQN("myfolder/firstFileOfSystemUser.txt");
        DocumentFQN petersInboxFileName = new DocumentFQN("peter/inboxfilename.txt");
        DocumentFQN petersTxFileName = new DocumentFQN("peterInternal/internalFilename.txt");

        LOGGER.debug("System user beginnt Transaction");
        transactionalDocumentSafeService.beginTransaction(systemUserIDAuth);

        LOGGER.debug("System user erstellt document");
        DocumentContent documentContent = new DocumentContent("content for in put box".getBytes());
        transactionalDocumentSafeService.txStoreDocument(systemUserIDAuth, new DSDocument(systemUserSourceDocFileName, documentContent));

        LOGGER.debug("System sucht Document");
        TxBucketContentFQN txBucketContentFQN = transactionalDocumentSafeService.txListDocuments(systemUserIDAuth, systemUserSourceDocFileName.getDocumentDirectory(), ListRecursiveFlag.TRUE);
        Assert.assertEquals(1, txBucketContentFQN.getFiles().size());

        LOGGER.debug("Peter beginnt Transaction");
        transactionalDocumentSafeService.beginTransaction(userIDAuth);

        LOGGER.debug("Peter hat noch nix in der Inbox");
        List<DocumentFQN> list = transactionalDocumentSafeService.nonTxListInbox(userIDAuth);
        Assert.assertEquals(0, list.size());

        LOGGER.debug("systemUser sendet Document an peter");
        transactionalDocumentSafeService.txMoveDocumentToInboxOfUser(systemUserIDAuth, userIDAuth.getUserID(), systemUserSourceDocFileName, petersInboxFileName, MoveType.MOVE);

        LOGGER.debug("peter lädt das document");
        transactionalDocumentSafeService.txMoveDocumentFromInbox(userIDAuth, petersInboxFileName, petersTxFileName);

        LOGGER.debug("peter liest das document aus seinem tx space");
        transactionalDocumentSafeService.txReadDocument(userIDAuth, petersTxFileName);

        transactionalDocumentSafeService.endTransaction(userIDAuth);
        transactionalDocumentSafeService.endTransaction(systemUserIDAuth);
    }

    @Test
    public void getCorrectVersionNumber() {
        transactionalDocumentSafeService.createUser(userIDAuth);
        DocumentFQN documentFQN = new DocumentFQN("testxTFolder/first.txt");
        DocumentContent documentContent = new DocumentContent("very first".getBytes());
        DSDocument document = new DSDocument(documentFQN, documentContent);

        TxDocumentFQNVersion version = null;
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            CatchException.catchException(() -> transactionalDocumentSafeService.getVersion(userIDAuth, documentFQN));
            Assert.assertTrue(CatchException.caughtException() != null);
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
            version = transactionalDocumentSafeService.getVersion(userIDAuth, documentFQN);
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            Assert.assertEquals(version, transactionalDocumentSafeService.getVersion(userIDAuth, documentFQN));
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            transactionalDocumentSafeService.txDeleteDocument(userIDAuth, documentFQN);
            CatchException.catchException(() -> transactionalDocumentSafeService.getVersion(userIDAuth, documentFQN));
            Assert.assertTrue(CatchException.caughtException() != null);
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
    }

    @Test
    public void testOverwrite() {


        transactionalDocumentSafeService.createUser(userIDAuth);
        DocumentFQN documentFQN = new DocumentFQN("testxTFolder/first.txt");
        DocumentContent documentContent = new DocumentContent("very first".getBytes());
        DSDocument document = new DSDocument(documentFQN, documentContent);

        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }

        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            BucketContentFQN bucketContentFQN = transactionalDocumentSafeService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
            Assert.assertEquals(1, bucketContentFQN.getFiles().size());
            Assert.assertEquals(documentFQN, bucketContentFQN.getFiles().get(0));
            DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertEquals(documentFQN, dsDocument.getDocumentFQN());
            Assert.assertArrayEquals(document.getDocumentContent().getValue(), dsDocument.getDocumentContent().getValue());
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
        DSDocument newDocument = null;
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            BucketContentFQN bucketContentFQN = transactionalDocumentSafeService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
            Assert.assertEquals(1, bucketContentFQN.getFiles().size());
            Assert.assertEquals(documentFQN, bucketContentFQN.getFiles().get(0));
            DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
            newDocument = new DSDocument(documentFQN, new DocumentContent("new content".getBytes()));
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, newDocument);
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertArrayEquals(newDocument.getDocumentContent().getValue(), dsDocument.getDocumentContent().getValue());
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
    }

    @Test(expected = TxInnerException.class)
    public void innerTxNotImplementedYet() {


        transactionalDocumentSafeService.createUser(userIDAuth);
        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        transactionalDocumentSafeService.beginTransaction(userIDAuth);
    }

    @Test
    public void testCreateAndChange() {


        transactionalDocumentSafeService.createUser(userIDAuth);
        DocumentFQN documentFQN = new DocumentFQN("testxTFolder/first.txt");
        DocumentContent documentContent1 = new DocumentContent("very first".getBytes());
        DocumentContent documentContent2 = new DocumentContent("second".getBytes());
        DSDocument document = new DSDocument(documentFQN, documentContent1);

        // Lege erste Version von first.txt an
        {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            LOGGER.debug("FIRST TXID ");
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, documentFQN));
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
            Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, documentFQN));
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }

        // Beginne neue Transaction
        {
            // Überschreibe erste version mit zweiter Version
            requestMemoryContext.switchToUser(2);
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            LOGGER.debug("SECOND TXID ");
            DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertEquals(new String(documentContent1.getValue()), new String(dsDocument.getDocumentContent().getValue()));
            DSDocument document2 = new DSDocument(documentFQN, documentContent2);
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document2);

            // Beginne dritte Transaktion VOR Ende der zweiten
            requestMemoryContext.switchToUser(3);
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            LOGGER.debug("THIRD TXID ");
            requestMemoryContext.switchToUser(2);
            transactionalDocumentSafeService.endTransaction(userIDAuth);

            // Beginne vierte Transaktion NACH Ende der zweiten
            requestMemoryContext.switchToUser(4);
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            LOGGER.debug("FOURTH TXID ");
        }

        {
            // dritte Tx muss noch ersten Inhalt lesen
            requestMemoryContext.switchToUser(3);
            DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertEquals(new String(documentContent1.getValue()), new String(dsDocument.getDocumentContent().getValue()));
        }

        {
            // vierte Tx muss schon zweiten Inhalt lesen
            requestMemoryContext.switchToUser(4);
            DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertEquals(new String(documentContent2.getValue()), new String(dsDocument.getDocumentContent().getValue()));
        }
        requestMemoryContext.switchToUser(4);
        BucketContentFQN list = transactionalDocumentSafeService.txListDocuments(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE);
        list.getFiles().forEach(file -> LOGGER.debug("file:" + file));
        Assert.assertEquals(1, list.getFiles().size());
    }

    @Test
    public void testDelete() {


        transactionalDocumentSafeService.createUser(userIDAuth);
        transactionalDocumentSafeService.beginTransaction(userIDAuth);

        int N = 5;
        // TODO actually a performance test
        N = 2;
        {
            // Nun erzeuge N verschiedene Datein in einem Verzeichnis
            for (int i = 0; i < N; i++) {
                DocumentFQN docFQN = new DocumentFQN("folder1/file_" + i + ".txt");
                DocumentContent docContent = new DocumentContent(("Content_" + i).getBytes());
                DSDocument doc = new DSDocument(docFQN, docContent);
                transactionalDocumentSafeService.txStoreDocument(userIDAuth, doc);
            }
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }

        {
            // Nun löschen der N verschiedenen Dateien in einer Transaktion
            requestMemoryContext.switchToUser(2);
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            requestMemoryContext.switchToUser(3);
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            requestMemoryContext.switchToUser(4);
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            requestMemoryContext.switchToUser(2);
            // Nun erzeuge N verschiedene Datein in einem Verzeichnis
            for (int i = 0; i < N; i++) {
                DocumentFQN docFQN = new DocumentFQN("folder1/file_" + i + ".txt");
                Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, docFQN));
                transactionalDocumentSafeService.txDeleteDocument(userIDAuth, docFQN);
                Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, docFQN));
            }
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }

        {
            // In der zuvor geöffneten Version müssen diese Dateien noch exisitieren,
            // da sie zum Zeitpunkt des Öffnens der TX noch exisitierten
            for (int i = 0; i < N; i++) {
                DocumentFQN docFQN = new DocumentFQN("folder1/file_" + i + ".txt");
                requestMemoryContext.switchToUser(3);
                Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, docFQN));
                requestMemoryContext.switchToUser(4);
                Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, docFQN));
            }
            requestMemoryContext.switchToUser(3);
            transactionalDocumentSafeService.txDeleteFolder(userIDAuth, new DocumentDirectoryFQN("folder1"));
            // Nun sind alle Dateien in der thirdTx weg, in der fourthTx aber noch da
            for (int i = 0; i < N; i++) {
                DocumentFQN docFQN = new DocumentFQN("folder1/file_" + i + ".txt");
                requestMemoryContext.switchToUser(3);
                Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, docFQN));
                requestMemoryContext.switchToUser(4);
                Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, docFQN));
            }
            requestMemoryContext.switchToUser(3);
            // this commit must not be successfull, because user 2 already commited the delete before.
            CatchException.catchException(() -> transactionalDocumentSafeService.endTransaction(userIDAuth));
            Assert.assertTrue(CatchException.caughtException() instanceof TxRacingConditionException);


            requestMemoryContext.switchToUser(4);
            // this commit must be successfull, because the tx did not write or delete any file
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
    }


    @Test(expected = TxNotActiveException.class)
    public void testEndTxTwice() {


        transactionalDocumentSafeService.createUser(userIDAuth);
        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        transactionalDocumentSafeService.endTransaction(userIDAuth);
        transactionalDocumentSafeService.endTransaction(userIDAuth);
    }

    @Test
    public void twoCommitsInARow() {
        transactionalDocumentSafeService.createUser(userIDAuth);
        DocumentFQN documentFQN = new DocumentFQN("testxTFolder/first.txt");
        DocumentContent documentContent = new DocumentContent("very first".getBytes());
        DSDocument document = new DSDocument(documentFQN, documentContent);

        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        LOGGER.debug("FIRST TXID ");
        Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, documentFQN));
        transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
        Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, documentFQN));
        transactionalDocumentSafeService.endTransaction(userIDAuth);

        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        LOGGER.debug("SECOND TXID ");
        DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
        Assert.assertEquals(new String(documentContent.getValue()), new String(dsDocument.getDocumentContent().getValue()));
        transactionalDocumentSafeService.endTransaction(userIDAuth);

        CatchException.catchException(() -> transactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN));
        Assert.assertTrue(CatchException.caughtException() instanceof TxNotActiveException);
    }
}



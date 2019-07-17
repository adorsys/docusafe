package de.adorsys.docusafe.business.impl;

import com.amazonaws.util.IOUtils;
import com.googlecode.catchexception.CatchException;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.service.impl.keystore.generator.ProviderUtils;
import lombok.extern.slf4j.Slf4j;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DSDocumentStream;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class DocumentServiceTest {
    DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
    UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));

    @Before
    public void before() {
        Provider bcProvider = ProviderUtils.bcProvider;
        service.createUser(userIDAuth);
    }

    @After
    public void after() {
        service.destroyUser(userIDAuth);
    }

    @Test
    public void createAndDestroy() {
        log.debug("OK ");
    }

    @Test
    public void storeAndReadOneDocument() {
        DocumentFQN documentFQN = new DocumentFQN("file1.txt");
        DocumentContent documentContent = new DocumentContent("affe".getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        service.storeDocument(userIDAuth, dsDocument);
        DSDocument dsDocument1 = service.readDocument(userIDAuth, documentFQN);
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());
    }

    @Test
    public void storeAndReadMoreDocuments() {
        DocumentDirectoryFQN root = new DocumentDirectoryFQN("affe");
        List<DSDocument> list = TestHelper.createDocuments(root, 2, 2, 3);
        List<DocumentFQN> created = new ArrayList<>();
        for (DSDocument dsDocument : list) {
            log.debug("store " + dsDocument.getDocumentFQN().toString());
            service.storeDocument(userIDAuth, dsDocument);
            created.add(dsDocument.getDocumentFQN());
            Assert.assertTrue(service.documentExists(userIDAuth, dsDocument.getDocumentFQN()));
        }
        List<DocumentFQN> listFound = service.list(userIDAuth, root, ListRecursiveFlag.TRUE);
        for (DocumentFQN doc : listFound) {
            log.debug("found:" + doc);
        }
        Assert.assertTrue(created.containsAll(listFound));
        Assert.assertTrue(listFound.containsAll(created));

    }


    @Test
    public void storeAndReadStream() {
        try {
            DocumentFQN documentFQN = new DocumentFQN("affe/file1.txt");
            DocumentContent documentContent = new DocumentContent("affe".getBytes());
            DSDocumentStream dsDocumentStream = new DSDocumentStream(documentFQN, new ByteArrayInputStream(documentContent.getValue()));
            service.storeDocumentStream(userIDAuth, dsDocumentStream);
            dsDocumentStream.getDocumentStream().close(); // not necessary as it is a bytestream

            DSDocumentStream dsDocumentStream1 = service.readDocumentStream(userIDAuth, documentFQN);
            byte[] readBytes = IOUtils.toByteArray(dsDocumentStream1.getDocumentStream());
            dsDocumentStream1.getDocumentStream().close();
            Assert.assertArrayEquals(documentContent.getValue(), readBytes);

        } catch (Exception e ) {
            throw BaseExceptionHandler.handle(e);
        }
    }


    @Test
    public void storeAndReadOneDocumentChangePasswordAndReadAgain() {
        Security.addProvider(new BouncyCastleProvider());

        DocumentFQN documentFQN = new DocumentFQN("file1.txt");
        DocumentContent documentContent = new DocumentContent("affe".getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        service.storeDocument(userIDAuth, dsDocument);
        DSDocument dsDocument1 = service.readDocument(userIDAuth, documentFQN);
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());

        ReadKeyPassword newReadKeyPassword = new ReadKeyPassword(UUID.randomUUID().toString());

        DocumentSafeService service2 = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
        service2.readDocument(userIDAuth, documentFQN);

        // --------------------
        service.changeUserPassword(userIDAuth, newReadKeyPassword);
        UserIDAuth newUserIDAuth = new UserIDAuth(userIDAuth.getUserID(), newReadKeyPassword);
        CatchException.catchException(() -> service.readDocument(userIDAuth, documentFQN));
        boolean exception1 = "java.io.IOException: javax.crypto.BadPaddingException: pad block corrupted".equals(CatchException.caughtException().getMessage());
        boolean exception2 = "java.security.UnrecoverableKeyException: no match".equals(CatchException.caughtException().getMessage());
        Assert.assertTrue(exception1 || exception2);
        Assert.assertTrue(CatchException.caughtException() != null);
        DSDocument dsDocument2 = service.readDocument(newUserIDAuth, documentFQN);
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument2.getDocumentContent().getValue());

        // ------
        // Service 2 has its own cache and thus will fail in the first
        DSDocument dsDocument3 = service2.readDocument(newUserIDAuth, documentFQN);
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument3.getDocumentContent().getValue());


        userIDAuth = newUserIDAuth;
    }

}

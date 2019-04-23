package org.adorsys.docusafe.business.impl;

import com.amazonaws.util.IOUtils;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DSDocumentStream;
import org.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DocumentServiceTest {
    DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
    UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));

    @Before
    public void before() {
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
        DocumentFQN documentFQN = new DocumentFQN("affe/file1.txt");
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
            DSDocumentStream dsDocumentStream1 = service.readDocumentStream(userIDAuth, documentFQN);
            byte[] readBytes = IOUtils.toByteArray(dsDocumentStream1.getDocumentStream());
            Assert.assertArrayEquals(documentContent.getValue(), readBytes);
        } catch (Exception e ) {
            throw BaseExceptionHandler.handle(e);
        }
    }
}

package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
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

@Slf4j
public class DocumentServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocumentServiceTest.class);

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
        LOGGER.debug("OK ");
    }

    @Test
    public void storeDocument() {
        LOGGER.debug("OK ");
        DocumentFQN documentFQN = new DocumentFQN("affe/file1.txt");
        DocumentContent documentContent = new DocumentContent("affe".getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        service.storeDocument(userIDAuth, dsDocument);
        DSDocument dsDocument1 = service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());

    }
}

package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.api.types.connection.FilesystemRootBucketName;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.DFSCredentials;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class RegisterDFSTest {
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
    public void registerAnotherDFS() {

        DocumentDirectoryFQN root = new DocumentDirectoryFQN("affe");
        List<DSDocument> list = TestHelper.createDocuments(root, 2, 2, 3);
        List<DocumentFQN> created = new ArrayList<>();
        for (DSDocument dsDocument : list) {
            service.storeDocument(userIDAuth, dsDocument);
        }

        DFSCredentials dfsCredentials = new DFSCredentials();
        FilesystemConnectionPropertiesImpl props = new FilesystemConnectionPropertiesImpl();
        props.setFilesystemRootBucketName(new FilesystemRootBucketName("target/another-root-bucket"));
        dfsCredentials.setFilesystem(new FilesystemConnectionPropertiesImpl(new FilesystemConnectionPropertiesImpl(props)));
        service.registerDFSCredentials(userIDAuth, dfsCredentials);

        // now retrieve a rondom document
        Random random = new Random();
        DSDocument documentFromMemory = list.get(random.nextInt(list.size()));
        DSDocument dsDocument = service.readDocument(userIDAuth, documentFromMemory.getDocumentFQN());
        Assert.assertArrayEquals(documentFromMemory.getDocumentContent().getValue(), dsDocument.getDocumentContent().getValue());
    }


    @Test
    public void createPuml() {

        DFSCredentials dfsCredentials = new DFSCredentials();
        FilesystemConnectionPropertiesImpl props = new FilesystemConnectionPropertiesImpl();
        props.setFilesystemRootBucketName(new FilesystemRootBucketName("target/another-root-bucket"));
        dfsCredentials.setFilesystem(new FilesystemConnectionPropertiesImpl(new FilesystemConnectionPropertiesImpl(props)));
        service.registerDFSCredentials(userIDAuth, dfsCredentials);
    }

}

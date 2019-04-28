package de.adorsys.docusafe.business.impl;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3RootBucketName;
import de.adorsys.dfs.connection.api.types.connection.FilesystemRootBucketName;
import de.adorsys.dfs.connection.api.types.properties.ConnectionProperties;
import de.adorsys.dfs.connection.impl.amazons3.AmazonS3ConnectionProperitesImpl;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DFSCredentials;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentDirectoryFQN;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
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

        ConnectionProperties props = DFSConnectionFactory.get().getConnectionProperties();
        props = changeRootDirectory( props, "for-one-user-only");
        DFSCredentials dfsCredentials = new DFSCredentials(props);
        service.registerDFSCredentials(userIDAuth, dfsCredentials);

        // now retrieve a rondom document
        Random random = new Random();
        DSDocument documentFromMemory = list.get(random.nextInt(list.size()));
        DSDocument dsDocument = service.readDocument(userIDAuth, documentFromMemory.getDocumentFQN());
        Assert.assertArrayEquals(documentFromMemory.getDocumentContent().getValue(), dsDocument.getDocumentContent().getValue());


    }


    @Test
    public void createPuml() {

        FilesystemConnectionPropertiesImpl props = new FilesystemConnectionPropertiesImpl();
        props.setFilesystemRootBucketName(new FilesystemRootBucketName("target/another-root-bucket"));
        DFSCredentials dfsCredentials = new DFSCredentials(props);
        service.registerDFSCredentials(userIDAuth, dfsCredentials);
    }

    private ConnectionProperties changeRootDirectory(ConnectionProperties props, String deeper) {
        if (props instanceof FilesystemConnectionPropertiesImpl) {
            FilesystemConnectionPropertiesImpl p = (FilesystemConnectionPropertiesImpl) props;
            String root = p.getFilesystemRootBucketName().getValue();
            root = root + BucketPath.BUCKET_SEPARATOR + deeper;
            p.setFilesystemRootBucketName(new FilesystemRootBucketName(root));
            return p;
        }

        if (props instanceof AmazonS3ConnectionProperitesImpl) {
            AmazonS3ConnectionProperitesImpl p = (AmazonS3ConnectionProperitesImpl) props;
            String root = p.getAmazonS3RootBucketName().getValue();
            root = root + BucketPath.BUCKET_SEPARATOR + deeper;
            p.setAmazonS3RootBucketName(new AmazonS3RootBucketName(root));
            return p;

        }
        throw new BaseException("unknown instance of properties:" + props);
    }
}

package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
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

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void storeMoreDocuments() {
        List<DSDocument> list = new ArrayList<>();
        DocumentDirectoryFQN root = new DocumentDirectoryFQN("affe");
        createDocuments(root, 2, 2, 3, list);
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

    private void createDocuments(DocumentDirectoryFQN directory, int numberOfDirectories, int numberOfFiles, int depth, List<DSDocument> list) {
        // create files in this directory
        for (int file = 0; file < numberOfFiles; file++) {
            list.add(createFile(directory, file));
        }

        if (depth == 0) {
            return;
        }

        // createFile for current directory
        for (int dir = 0; dir < numberOfDirectories; dir++) {
            DocumentDirectoryFQN subdir = directory.addDirectory("subdir_" + dir);
            createDocuments(subdir, numberOfDirectories, numberOfFiles, depth-1, list);
        }
    }

    private DSDocument createFile(DocumentDirectoryFQN directoryFQN, int numberOfFile) {
        DocumentFQN documentFQN = directoryFQN.addName("file" + numberOfFile + "txt");
        DocumentContent documentContent = new DocumentContent(("my name is " + documentFQN.toString()).getBytes());
        return new DSDocument(documentFQN, documentContent);
    }
}

package de.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.business.types.MoveType;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

@Slf4j
public class InboxTest {
    DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
    UserIDAuth userIDAuthA = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));
    UserIDAuth userIDAuthB = new UserIDAuth(new UserID("francis"), new ReadKeyPassword("another"));

    @Before
    public void before() {
        service.createUser(userIDAuthA);
        service.createUser(userIDAuthB);
    }

    @After
    public void after() {
        service.destroyUser(userIDAuthA);
        service.destroyUser(userIDAuthB);
    }

    @Test
    public void inbox() {
        DocumentFQN documentFQN = new DocumentFQN("affe/file1.txt");
        DocumentFQN inboxFQN1 = new DocumentFQN("secret1.txt");
        DocumentFQN inboxFQN2 = new DocumentFQN("secret2.txt");
        DocumentFQN documentFQN2 = new DocumentFQN("affe/file2.txt");
        DocumentContent documentContent = new DocumentContent("affe".getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        service.storeDocument(userIDAuthA, dsDocument);

        // copy document
        service.moveDocumnetToInboxOfUser(userIDAuthA, userIDAuthB.getUserID(), documentFQN, inboxFQN1, MoveType.KEEP_COPY);

        // receive document
        List<DocumentFQN> inboxList = service.listInbox(userIDAuthB);
        inboxList.forEach(el -> log.debug("found in inbox:" + el.toString()));
        Assert.assertTrue(inboxList.contains(inboxFQN1));
        DSDocument dsDocument1 = service.moveDocumentFromInbox(userIDAuthB, inboxFQN1, documentFQN);

        // compare documents
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());

        // inbox empty
        Assert.assertTrue(service.listInbox(userIDAuthB).isEmpty());

        // move document
        service.moveDocumnetToInboxOfUser(userIDAuthA, userIDAuthB.getUserID(), documentFQN, inboxFQN2, MoveType.MOVE);

        // receive document again
        inboxList = service.listInbox(userIDAuthB);
        Assert.assertTrue(inboxList.contains(inboxFQN2));
        DSDocument dsDocument2 = service.moveDocumentFromInbox(userIDAuthB, inboxFQN2, documentFQN2);

        // compare documents
        Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument2.getDocumentContent().getValue());

        // inbox empty
        Assert.assertTrue(service.listInbox(userIDAuthB).isEmpty());

        // file does no more exist for userA
        Assert.assertTrue(!service.documentExists(userIDAuthA, documentFQN));

        // file exists two times for user B
        Assert.assertTrue(service.documentExists(userIDAuthB, documentFQN));
        Assert.assertTrue(service.documentExists(userIDAuthB, documentFQN2));
    }

}

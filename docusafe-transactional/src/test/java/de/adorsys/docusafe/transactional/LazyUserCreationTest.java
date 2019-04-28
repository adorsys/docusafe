package de.adorsys.docusafe.transactional;

import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 28.11.18 17:11.
 */
public class LazyUserCreationTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionalDocumentSafeServiceTest.class);

    // TODO DOC-73 wurde nicht umgesetzt, da Francis in Telco mit Peter entschieden hat, dass
    // begin und endTransaction NICHT automatisch aus dem Restlayer erfolgen sollten
    // @Test
    public void createUserAfterBegin() {

        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        transactionalDocumentSafeService.createUser(userIDAuth);
        DocumentFQN documentFQN = new DocumentFQN("testxTFolder/first.txt");
        DocumentContent documentContent = new DocumentContent("very first".getBytes());
        DSDocument document = new DSDocument(documentFQN, documentContent);

        Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, documentFQN));
        transactionalDocumentSafeService.txStoreDocument(userIDAuth, document);
        Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, documentFQN));
        transactionalDocumentSafeService.endTransaction(userIDAuth);
    }
}

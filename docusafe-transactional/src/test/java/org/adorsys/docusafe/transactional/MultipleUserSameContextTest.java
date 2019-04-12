package org.adorsys.docusafe.transactional;

import com.googlecode.catchexception.CatchException;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 01.02.19 10:59.
 */
public class MultipleUserSameContextTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(MultipleUserSameContextTest.class);

    // DOC-80
    @Test
    public void twoUsersCreateDocumenteInTheirOwnScopeButWithTheSameRequestContext() {
        {
            transactionalDocumentSafeService.createUser(userIDAuth);
            transactionalDocumentSafeService.createUser(systemUserIDAuth);

            DSDocument document1 = createDocument("file1");
            DSDocument document2 = createDocument("file2");

            LOGGER.debug("user1 starts TX");
            transactionalDocumentSafeService.beginTransaction(userIDAuth);

            LOGGER.debug("user1 cant see the not yet created document " + document1.getDocumentFQN());
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, document1.getDocumentFQN()));

            LOGGER.debug("user1 creates " + document1.getDocumentFQN());
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document1);

            LOGGER.debug("user1 can see his own documents " + document1.getDocumentFQN());
            Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, document1.getDocumentFQN()));

            LOGGER.debug("user2 starts TX");
            transactionalDocumentSafeService.beginTransaction(systemUserIDAuth);

            LOGGER.debug("user2 cant see documents of user1");
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(systemUserIDAuth, document1.getDocumentFQN()));

            LOGGER.debug("user1 ends TX");
            transactionalDocumentSafeService.endTransaction(userIDAuth);

            LOGGER.debug("user2 still cant see documents of user1");
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(systemUserIDAuth, document1.getDocumentFQN()));

            LOGGER.debug("user2 creates " + document2.getDocumentFQN());
            transactionalDocumentSafeService.txStoreDocument(systemUserIDAuth, document2);

            LOGGER.debug("user2 cant see the new document");
            Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(systemUserIDAuth, document2.getDocumentFQN()));

            LOGGER.debug("user1 cant do anything withoud opening another tx");
            CatchException.catchException(() -> transactionalDocumentSafeService.txDocumentExists(userIDAuth, document1.getDocumentFQN()));
            Assert.assertNotNull(CatchException.caughtException());

            LOGGER.debug("user1 starts another TX");
            transactionalDocumentSafeService.beginTransaction(userIDAuth);

            LOGGER.debug("user1 can see his own documents " + document1.getDocumentFQN());
            Assert.assertTrue(transactionalDocumentSafeService.txDocumentExists(userIDAuth, document1.getDocumentFQN()));

            LOGGER.debug("user1 cant see documents of user2 " + document2.getDocumentFQN());
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, document2.getDocumentFQN()));

            LOGGER.debug("user2 ends TX");
            transactionalDocumentSafeService.endTransaction(systemUserIDAuth);

            LOGGER.debug("user1 still cant see documents of user2 " + document2.getDocumentFQN());
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, document2.getDocumentFQN()));

            LOGGER.debug("user1 ends TX");
            transactionalDocumentSafeService.endTransaction(userIDAuth);

            LOGGER.debug("user1 starts another TX");
            transactionalDocumentSafeService.beginTransaction(userIDAuth);

            LOGGER.debug("user2 starts another TX");
            transactionalDocumentSafeService.beginTransaction(systemUserIDAuth);

            LOGGER.debug("user1 will never see documents of user2 " + document2.getDocumentFQN());
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(userIDAuth, document2.getDocumentFQN()));

            LOGGER.debug("user2 will never see documents of user1 " + document1.getDocumentFQN());
            Assert.assertFalse(transactionalDocumentSafeService.txDocumentExists(systemUserIDAuth, document1.getDocumentFQN()));
        }
    }

}

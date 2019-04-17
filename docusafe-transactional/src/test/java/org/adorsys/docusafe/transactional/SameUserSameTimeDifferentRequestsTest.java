package org.adorsys.docusafe.transactional;

import com.googlecode.catchexception.CatchException;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 07.03.19 11:22.
 */
public class SameUserSameTimeDifferentRequestsTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(SameUserSameTimeDifferentRequestsTest.class);

    SimpleRequestMemoryContextImpl secondRequestMemoryContext = new SimpleRequestMemoryContextImpl();
    TransactionalDocumentSafeService secondTransactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(secondRequestMemoryContext, new DocumentSafeServiceImpl(DFSConnectionFactory.get()));

    @Test
    @SuppressWarnings("Duplicates")
    public void a() {
        transactionalDocumentSafeService.createUser(userIDAuth);

        DSDocument document1 = createDocument("file1");
        DSDocument document2 = createDocument("file2");
        DSDocument document3 = createDocument("file3");
        DSDocument document4 = createDocument("file4");
        DSDocument document5 = createDocument("file5");

        {
            LOGGER.debug("user1 starts TX");
            transactionalDocumentSafeService.beginTransaction(userIDAuth);

            LOGGER.debug("user1 creates " + document1.getDocumentFQN());
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document1);

            LOGGER.debug("user1 ends TX");
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }
        {
            LOGGER.debug("user1 starts TX");
            transactionalDocumentSafeService.beginTransaction(userIDAuth);

            LOGGER.debug("user1 in a new context starts another TX");
            secondTransactionalDocumentSafeService.beginTransaction(userIDAuth);

            LOGGER.debug("user1 in the second tx creates " + document2.getDocumentFQN());
            secondTransactionalDocumentSafeService.txStoreDocument(userIDAuth, document2);

            LOGGER.debug("user1 in the first tx creates " + document2.getDocumentFQN());
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, document2);

            LOGGER.debug("user1 first tx ends TX");
            transactionalDocumentSafeService.endTransaction(userIDAuth);

            LOGGER.debug("user1 second tx ends TX and gets exception");
            CatchException.catchException(() ->secondTransactionalDocumentSafeService.endTransaction(userIDAuth));
            Assert.assertNotNull(CatchException.caughtException());
        }
        {
            {
                LOGGER.debug("user1 in a new context starts another TX");
                secondTransactionalDocumentSafeService.beginTransaction(userIDAuth);
            }
            {
                LOGGER.debug("user1 starts TX");
                transactionalDocumentSafeService.beginTransaction(userIDAuth);

                LOGGER.debug("user1 in the first tx creates " + document2.getDocumentFQN());
                transactionalDocumentSafeService.txStoreDocument(userIDAuth, document2);

                LOGGER.debug("user1 in the first tx creates " + document5.getDocumentFQN());
                transactionalDocumentSafeService.txStoreDocument(userIDAuth, document5);

                LOGGER.debug("user1 first tx ends TX");
                transactionalDocumentSafeService.endTransaction(userIDAuth);
            }
            {
                LOGGER.debug("user1 starts TX");
                transactionalDocumentSafeService.beginTransaction(userIDAuth);

                LOGGER.debug("user1 in the first tx creates " + document2.getDocumentFQN());
                transactionalDocumentSafeService.txStoreDocument(userIDAuth, document2);

                LOGGER.debug("user1 in the first tx creates " + document3.getDocumentFQN());
                transactionalDocumentSafeService.txStoreDocument(userIDAuth, document3);

                LOGGER.debug("user1 in the first tx DELETES " + document5.getDocumentFQN());
                transactionalDocumentSafeService.txDeleteDocument(userIDAuth, document5.getDocumentFQN());

                LOGGER.debug("user1 first tx ends TX");
                transactionalDocumentSafeService.endTransaction(userIDAuth);
            }
            {
                LOGGER.debug("user1 in the second context creates " + document4.getDocumentFQN());
                secondTransactionalDocumentSafeService.txStoreDocument(userIDAuth, document4);

                LOGGER.debug("user1 second context ends TX");
//                secondTransactionalDocumentSafeService.endTransaction(userIDAuth);
                CatchException.catchException(() ->secondTransactionalDocumentSafeService.endTransaction(userIDAuth));
                LOGGER.info("should be reached");
            }
        }


    }


}

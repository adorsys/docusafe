package de.adorsys.docusafe.transactional;

import com.googlecode.catchexception.CatchException;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 15.06.18 at 12:52.
 */
public class TransactionalDocumentSafeServiceBaseTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(TransactionalDocumentSafeServiceBaseTest.class);

    protected SimpleRequestMemoryContextImpl requestMemoryContext = new SimpleRequestMemoryContextImpl();
    protected DocumentSafeService dss = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
    protected TransactionalDocumentSafeService transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(requestMemoryContext, dss);
    protected NonTransactionalDocumentSafeService nonTransactionalDocumentSafeService = transactionalDocumentSafeService;
    protected UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("password"));
    protected UserIDAuth systemUserIDAuth = new UserIDAuth(new UserID("system"), new ReadKeyPassword("systemPassword"));

    @Before
    public void preTestBase() {
        LOGGER.debug("preTestBase");

        DFSConnectionFactory.get().deleteDatabase();
        LOGGER.debug("TransactionalDcoumentSafeService is " + transactionalDocumentSafeService.getClass().getName());
    }

    @After
    public void afterTestBase() {
        LOGGER.debug("afterTestBase");
        if (transactionalDocumentSafeService.userExists(userIDAuth.getUserID())) {
            CatchException.catchException(() -> transactionalDocumentSafeService.destroyUser(userIDAuth));
        }
        if (transactionalDocumentSafeService.userExists(systemUserIDAuth.getUserID())) {
            CatchException.catchException(() -> transactionalDocumentSafeService.destroyUser(systemUserIDAuth));
        }
    }

    protected DSDocument createDocument(String name) {
        DocumentFQN documentFQN = new DocumentFQN(name);
        DocumentContent documentContent = new DocumentContent(("CONTENT OF FILE " + name).getBytes());
        return new DSDocument(documentFQN, documentContent);
    }


}

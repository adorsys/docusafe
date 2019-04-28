package de.adorsys.docusafe.cached.transactional.impl;

import de.adorsys.docusafe.transactional.TransactionalDocumentSafeServiceTest;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 22.02.19 19:12.
 */
public class CachedTransactionalDocumentSafeServiceTest extends TransactionalDocumentSafeServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(CachedTransactionalDocumentSafeServiceTest.class);
    private TransactionalDocumentSafeServiceTestWrapper wrapper = null;

    @Before
    public void preTestCached() {
        LOGGER.debug("preTestCached changed transactionalDocumentSafeService");

        // erst mal einen neuen TransactionalDocumentSafeService anlegen
        transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(requestMemoryContext, dss);

        // erst mal machen wir aus der transactionalDocumentSafeService eine cachedTransactionalDocumentSafeService;
        transactionalDocumentSafeService = new CachedTransactionalDocumentSafeServiceImpl(requestMemoryContext, transactionalDocumentSafeService, dss);

        // diese wrappen wir
        wrapper = new TransactionalDocumentSafeServiceTestWrapper(transactionalDocumentSafeService);

        // und die gewrappte geben wir an den test
        transactionalDocumentSafeService = wrapper;

        // und der nichttransaktionale teil wird ebenfalls mit dem Wrapper versorgt
        nonTransactionalDocumentSafeService = transactionalDocumentSafeService;
    }

    @After
    public void afterTestCached() {
        LOGGER.debug("afterTestCached " + transactionalDocumentSafeService.toString());
    }


    @Test
    @Override
    public void sendDocumentFromSystemUserToPeter() {
        super.sendDocumentFromSystemUserToPeter();
    }
}

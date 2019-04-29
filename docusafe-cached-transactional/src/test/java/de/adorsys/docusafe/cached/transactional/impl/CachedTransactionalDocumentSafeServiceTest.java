package de.adorsys.docusafe.cached.transactional.impl;

import de.adorsys.docusafe.transactional.TransactionalDocumentSafeServiceTest;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 22.02.19 19:12.
 */
@RunWith(value = PowerMockRunner.class)
@PowerMockIgnore("javax.*")
public class CachedTransactionalDocumentSafeServiceTest extends TransactionalDocumentSafeServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(CachedTransactionalDocumentSafeServiceTest.class);

    @Before
    public void preTestCached() {
        LOGGER.debug("preTestCached changed transactionalDocumentSafeService");

        // erst mal einen neuen TransactionalDocumentSafeService anlegen
        transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(requestMemoryContext, dss);

        // erst mal machen wir aus der transactionalDocumentSafeService eine cachedTransactionalDocumentSafeService;
        transactionalDocumentSafeService = new CachedTransactionalDocumentSafeServiceImpl(requestMemoryContext, transactionalDocumentSafeService, dss);

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

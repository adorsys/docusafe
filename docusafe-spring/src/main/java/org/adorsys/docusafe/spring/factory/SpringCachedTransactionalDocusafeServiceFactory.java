package org.adorsys.docusafe.spring.factory;

import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import org.adorsys.docusafe.cached.transactional.impl.CachedTransactionalDocumentSafeServiceImpl;
import org.adorsys.docusafe.spring.SimpleRequestMemoryContextImpl;
import org.adorsys.docusafe.transactional.RequestMemoryContext;
import org.adorsys.docusafe.transactional.TransactionalDocumentSafeService;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 14.11.18 20:24.
 */
public class SpringCachedTransactionalDocusafeServiceFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpringCachedTransactionalDocusafeServiceFactory.class);
    private SpringDFSConnectionFactory connectionFactory;
    private static int instanceCounter = 0;
    final private int instanceId;
    private Map<String, CachedTransactionalDocumentSafeService> map = new HashMap<>();


    public SpringCachedTransactionalDocusafeServiceFactory(SpringDFSConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
        instanceId = ++instanceCounter;
        if (instanceId > 1) {
            LOGGER.warn("Expected just to exist exactly one Factory. But this is Instance No: " + instanceId);
        }
    }

    public CachedTransactionalDocumentSafeService getCachedTransactionalDocumentSafeServiceWithSubdir(String basedir) {
        if (map.containsKey(basedir)) {
            LOGGER.info("Connection for " + (basedir==null ? "default" : basedir) + " is known. Singleton is returned");
            return map.get(basedir);
        }
        LOGGER.info("getExtendedStoreConnection");
        DFSConnection extendedStoreConnection = connectionFactory.getExtendedStoreConnectionWithSubDir(basedir);
        LOGGER.info(CachedTransactionalDocumentSafeService.class.getName() + " is required as @Bean");
        LOGGER.debug("create documentSafeService");
        DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(extendedStoreConnection);
        RequestMemoryContext requestContext = new SimpleRequestMemoryContextImpl();
        LOGGER.debug("create transactionalDocumentSafeService");
        TransactionalDocumentSafeService transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(requestContext, documentSafeService);
        LOGGER.debug("create cachedTransactionalDocumentSafeService");
        CachedTransactionalDocumentSafeService cachedTransactionalDocumentSafeService = new CachedTransactionalDocumentSafeServiceImpl(requestContext, transactionalDocumentSafeService, documentSafeService);
        map.put(basedir, cachedTransactionalDocumentSafeService);
        return cachedTransactionalDocumentSafeService;
    }

}

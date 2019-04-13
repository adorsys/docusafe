package org.adorsys.docusafe.spring.config;

import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import org.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import org.adorsys.docusafe.spring.factory.SpringCachedTransactionalDocusafeServiceFactory;
import org.adorsys.docusafe.spring.factory.SpringDFSConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by peter on 02.10.18.
 */
@Configuration
public class DocusafeSpringBeans {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocusafeSpringBeans.class);

    public DocusafeSpringBeans() {
        LOGGER.info("INIT");
    }

    @Bean
    public DFSConnection extendedStoreConnection(SpringDFSConnectionFactory factory) {
        LOGGER.info(DFSConnection.class.getName() + " is required as @Bean");
        return factory.getExtendedStoreConnectionWithSubDir(null);
    }

    @Bean
    public SpringDFSConnectionFactory springExtendedStoreConnectionFactory(SpringDFSConnectionProperties properties) {
        LOGGER.info(SpringDFSConnectionFactory.class.getName() + " is required as @Bean");
        return new SpringDFSConnectionFactory(properties);
    }

    @Bean
    public SpringCachedTransactionalDocusafeServiceFactory springCachedTransactionalDocusafeServiceFactory(SpringDFSConnectionFactory connectionFactory) {
        LOGGER.info(SpringCachedTransactionalDocusafeServiceFactory.class.getName() + " is required as @Bean");
        return new SpringCachedTransactionalDocusafeServiceFactory(connectionFactory);
    }

    @Bean
    public CachedTransactionalDocumentSafeService cachedTransactionalDocumentSafeService(
            SpringCachedTransactionalDocusafeServiceFactory springCachedTransactionalDocusafeServiceFactory,
            @Value("${docusafe.cache:true}") Boolean withCache
    ) {
        LOGGER.info(CachedTransactionalDocumentSafeService.class.getName() + " is required as @Bean");
        return springCachedTransactionalDocusafeServiceFactory.getCachedTransactionalDocumentSafeServiceWithSubdir(null);
    }
}

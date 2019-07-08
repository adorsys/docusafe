package de.adorsys.docusafe.spring.config;

import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import de.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import de.adorsys.docusafe.spring.factory.SpringCachedTransactionalDocusafeServiceFactory;
import de.adorsys.docusafe.spring.factory.SpringDFSConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by peter on 02.10.18.
 */
@EnableConfigurationProperties({

        SpringFilesystemConnectionProperties.class,
        SpringAmazonS3ConnectionProperties.class}
)
@Configuration
public class DocusafeSpringBeans {
    private final static Logger LOGGER = LoggerFactory.getLogger(DocusafeSpringBeans.class);

    public DocusafeSpringBeans() {
        LOGGER.info("INIT");
    }

    @Bean
    public DFSConnection DFSConnection(SpringDFSConnectionFactory factory) {
        LOGGER.info(DFSConnection.class.getName() + " is provide as @Bean");
        return factory.getDFSConnectionWithSubDir(null);
    }

    @Bean
    public SpringDFSConnectionFactory springDFSConnectionFactory(SpringDFSConnectionProperties properties) {
        LOGGER.info(SpringDFSConnectionFactory.class.getName() + " is provided as @Bean");
        return new SpringDFSConnectionFactory(properties);
    }

    @Bean
    public DocumentSafeService documentSafeService(SpringDFSConnectionFactory dfsFactory) {
        LOGGER.info(SpringDFSConnectionProperties.class.getName() + " is provided as @Bean");
        return new DocumentSafeServiceImpl(dfsFactory.getDFSConnectionWithSubDir(null));
    }

    @Bean
    public SpringCachedTransactionalDocusafeServiceFactory springCachedTransactionalDocusafeServiceFactory(SpringDFSConnectionFactory connectionFactory) {
        LOGGER.info(SpringCachedTransactionalDocusafeServiceFactory.class.getName() + " is provided as @Bean");
        return new SpringCachedTransactionalDocusafeServiceFactory(connectionFactory);
    }

    @Bean
    public CachedTransactionalDocumentSafeService cachedTransactionalDocumentSafeService(
            SpringCachedTransactionalDocusafeServiceFactory springCachedTransactionalDocusafeServiceFactory,
            @Value("${docusafe.cache:true}") Boolean withCache
    ) {
        LOGGER.info(CachedTransactionalDocumentSafeService.class.getName() + " is provided as @Bean");
        return springCachedTransactionalDocusafeServiceFactory.getCachedTransactionalDocumentSafeServiceWithSubdir(null);
    }
}

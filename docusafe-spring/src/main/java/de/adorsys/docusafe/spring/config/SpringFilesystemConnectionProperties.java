package de.adorsys.docusafe.spring.config;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.api.types.connection.FilesystemRootBucketName;
import de.adorsys.dfs.connection.api.types.properties.FilesystemConnectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Created by peter on 05.10.18.
 */
@Component
@ConfigurationProperties(prefix = "docusafe.storeconnection.filesystem")
@Validated
public class SpringFilesystemConnectionProperties extends SpringConnectionPropertiesImpl implements FilesystemConnectionProperties {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpringFilesystemConnectionProperties.class);
    public final static String template = "\n" +
            "docusafe:\n" +
            "  storeconnection:\n" +
            "    filesystem:\n" +
            "      rootbucket: (mandatory)\n" +
            SpringConnectionPropertiesImpl.template;


    private String rootbucket;

    @Override
    public FilesystemRootBucketName getFilesystemRootBucketName() {
        if (rootbucket == null) {
            throw new BaseException("basedir must not be null");
        }
        LOGGER.debug("basedir:" + new FilesystemRootBucketName(rootbucket));
        return new FilesystemRootBucketName(rootbucket);
    }

    public void setRootbucket(String rootbucket) {
        this.rootbucket = rootbucket;
    }
}

package org.adorsys.docusafe.spring.config;

import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3AccessKey;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3Region;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3RootBucketName;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3SecretKey;
import de.adorsys.dfs.connection.api.types.properties.AmazonS3ConnectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URL;

/**
 * Created by peter on 04.10.18.
 */
@Component
@ConfigurationProperties(prefix = "docusafe.storeconnection.amazons3")
@Validated
public class SpringAmazonS3ConnectionProperties extends SpringConnectionPropertiesImpl implements AmazonS3ConnectionProperties {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpringAmazonS3ConnectionProperties.class);
    public final static String template = "\n" +
            "docusafe:\n" +
            "  storeconnection:\n" +
            "    amazons3:\n" +
            "      url: (mandatory)\n" +
            "      accesskey: (mandatory)\n" +
            "      secretkey: (mandatory)\n" +
            "      region: (optional)\n" +
            "      rootbucket: (optional)\n" +
            SpringConnectionPropertiesImpl.template;

    private String url;
    private String accesskey;
    private String secretkey;
    private String region = defaultRegion.getValue();
    @Nullable
    private String rootbucket = defaultRootBucketName.getValue();

    @Override
    public URL getUrl() {
        try {
            LOGGER.debug("url is:\"" + url + "\"");
            return new URL(url);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    @Override
    public AmazonS3AccessKey getAmazonS3AccessKey() {
        LOGGER.debug("accesskey is:\"" + new AmazonS3AccessKey(accesskey) + "\"");
        return new AmazonS3AccessKey(accesskey);
    }

    @Override
    public AmazonS3SecretKey getAmazonS3SecretKey() {
        LOGGER.debug("secretkey is:\"" + new AmazonS3SecretKey(secretkey) + "\"");
        return new AmazonS3SecretKey(secretkey);
    }

    @Override
    public AmazonS3Region getAmazonS3Region() {
        LOGGER.debug("region is:\"" + new AmazonS3Region(region) + "\"");
        return new AmazonS3Region(region);
    }

    @Override
    public AmazonS3RootBucketName getAmazonS3RootBucketName() {
        LOGGER.debug("rootbucket is:\"" + rootbucket + "\"");
        return new AmazonS3RootBucketName(rootbucket);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setSecretkey(String secretkey) {
        this.secretkey = secretkey;
    }

    public void setAccesskey(String accesskey) {
        this.accesskey = accesskey;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setRootbucket(String rootbucket) {
        this.rootbucket = rootbucket;
    }
}

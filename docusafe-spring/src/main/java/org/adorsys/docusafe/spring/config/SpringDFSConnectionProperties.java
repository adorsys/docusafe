package org.adorsys.docusafe.spring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Created by peter on 05.10.18.
 */
@Component
@ConfigurationProperties(prefix = "docusafe.storeconnection")
@Validated
public class SpringDFSConnectionProperties {
    @Nullable
    private SpringAmazonS3ConnectionProperties amazons3;
    @Nullable
    private SpringFilesystemConnectionProperties filesystem;


    public SpringAmazonS3ConnectionProperties getAmazons3() {
        return amazons3;
    }

    public void setAmazons3(SpringAmazonS3ConnectionProperties amazons3) {
        this.amazons3 = amazons3;
    }

    public SpringFilesystemConnectionProperties getFilesystem() {
        return filesystem;
    }

    public void setFilesystem(SpringFilesystemConnectionProperties filesystem) {
        this.filesystem = filesystem;
    }

}

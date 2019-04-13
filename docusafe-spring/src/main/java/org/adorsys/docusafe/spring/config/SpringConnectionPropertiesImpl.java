package org.adorsys.docusafe.spring.config;

import de.adorsys.dfs.connection.api.types.BucketPathEncryptionPassword;
import de.adorsys.dfs.connection.api.types.properties.BucketPathEncryptionFilenameOnly;
import de.adorsys.dfs.connection.api.types.properties.ConnectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * Created by peter on 05.10.18.
 */
public class SpringConnectionPropertiesImpl implements ConnectionProperties {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpringConnectionPropertiesImpl.class);
    protected final static String template = "      encryptionpassword: (optional. null means no ecryption)\n"
            + "      encryptionfilenameonly: (optional. TRUE means, path is not encrypted)";


    @Nullable
    private String encryptionpassword = defaultEncryptionPassword.getValue();

    @Nullable
    private String encryptionfilenameonly = defaultBucketPathEncryptionFilenameOnly.toString();

    @Override
    public BucketPathEncryptionPassword getBucketPathEncryptionPassword() {
        if (encryptionpassword == null || encryptionpassword.length() == 0 || encryptionpassword.equalsIgnoreCase("null")) {
            LOGGER.debug("encryptionpassword is:null");
            return null;
        }
        LOGGER.debug("encryptionpassword is:\"" + new BucketPathEncryptionPassword(encryptionpassword) + "\"");
        return new BucketPathEncryptionPassword(encryptionpassword);
    }

    public void setEncryptionpassword(String encryptionpassword) {
        this.encryptionpassword = encryptionpassword;
    }

    @Override
    public BucketPathEncryptionFilenameOnly getBucketPathEncryptionFilenameOnly() {
        if (encryptionfilenameonly == null) {
            return defaultBucketPathEncryptionFilenameOnly;
        }
        if (encryptionfilenameonly.equalsIgnoreCase("true")) {
            return BucketPathEncryptionFilenameOnly.TRUE;
        }
        return BucketPathEncryptionFilenameOnly.FALSE;
    }

    public void setEncryptionfilenameonly(String encryptionfilenameonly) {
        this.encryptionfilenameonly = encryptionfilenameonly;
    }
}

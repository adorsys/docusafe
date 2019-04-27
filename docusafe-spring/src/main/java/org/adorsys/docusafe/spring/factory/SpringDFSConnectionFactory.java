package org.adorsys.docusafe.spring.factory;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3RootBucketName;
import de.adorsys.dfs.connection.api.types.connection.FilesystemRootBucketName;
import de.adorsys.dfs.connection.impl.amazons3.AmazonS3ConnectionProperitesImpl;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import org.adorsys.docusafe.spring.config.SpringAmazonS3ConnectionProperties;
import org.adorsys.docusafe.spring.config.SpringDFSConnectionProperties;
import org.adorsys.docusafe.spring.config.SpringFilesystemConnectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 14.11.18 12:05.
 */
public class SpringDFSConnectionFactory {
    private final static Logger LOGGER = LoggerFactory.getLogger(SpringDFSConnectionFactory.class);
    private SpringDFSConnectionProperties wiredProperties;
    private static int instanceCounter = 0;
    final private int instanceId;
    private Map<String, DFSConnection> map = new HashMap<>();

    public SpringDFSConnectionFactory(SpringDFSConnectionProperties wiredProperties) {
        this.wiredProperties = wiredProperties;
        instanceId = ++instanceCounter;
        if (instanceId > 1) {
            LOGGER.warn("Expected just to exist exactly one Factory. But this is Instance No: " + instanceId);
        }
    }

    public DFSConnection getDFSConnectionWithSubDir(String basedir) {
        if (map.containsKey(basedir)) {
            LOGGER.info("Connection for " + (basedir==null ? "default" : basedir) + " is known. Singleton is returned");
            return map.get(basedir);
        }
        if (wiredProperties.getFilesystem() != null) {
            FilesystemConnectionPropertiesImpl properties = new FilesystemConnectionPropertiesImpl(wiredProperties.getFilesystem());
            if (basedir != null) {
                String origName = properties.getFilesystemRootBucketName().getValue();
                String newName = origName + "." + basedir;
                properties.setFilesystemRootBucketName(new FilesystemRootBucketName(newName));
            }
            LOGGER.debug("jetzt filesystem");
            map.put(basedir, DFSConnectionFactory.get(properties));
        } else if (wiredProperties.getAmazons3() != null) {
            AmazonS3ConnectionProperitesImpl properties = new AmazonS3ConnectionProperitesImpl(wiredProperties.getAmazons3());
            if (basedir != null) {
                String origName = properties.getAmazonS3RootBucketName().getValue();
                String newName = origName + "." + basedir;
                properties.setAmazonS3RootBucketName(new AmazonS3RootBucketName(newName));
            }
            LOGGER.debug("jetzt amazon");
            map.put(basedir, DFSConnectionFactory.get(properties));
        } else {
            String emessage = "at least filesystem, amazons3, minio or mongo has to be specified with ";
            String message = emessage +
                    SpringFilesystemConnectionProperties.template +
                    SpringAmazonS3ConnectionProperties.template;
            LOGGER.error(message);
            throw new BaseException(emessage);
        }
        return map.get(basedir);
    }


}

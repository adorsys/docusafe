package org.adorsys.docusafe.business.types;

import de.adorsys.dfs.connection.api.filesystem.FilesystemConnectionPropertiesImpl;
import de.adorsys.dfs.connection.api.types.connection.AmazonS3RootBucketName;
import de.adorsys.dfs.connection.api.types.connection.FilesystemRootBucketName;
import de.adorsys.dfs.connection.api.types.properties.ConnectionProperties;
import de.adorsys.dfs.connection.impl.amazons3.AmazonS3ConnectionProperitesImpl;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.adorsys.docusafe.service.api.types.UserID;

@Data
@Getter
@Setter
public class DFSCredentials {
    private FilesystemConnectionPropertiesImpl filesystem = null;
    private AmazonS3ConnectionProperitesImpl amazons3 = null;

    public DFSCredentials() {

    }

    public DFSCredentials(DFSCredentials dfsCredentials) {
        if (dfsCredentials.filesystem != null) {
            filesystem = new FilesystemConnectionPropertiesImpl(dfsCredentials.filesystem);
        }
        if (dfsCredentials.amazons3 != null) {
            amazons3 = new AmazonS3ConnectionProperitesImpl(dfsCredentials.amazons3);
        }
    }
    public void setRootBucket(UserID userID) {

        if (getFilesystem() != null) {
            String root = getFilesystem().getFilesystemRootBucketName().getValue();
            root += ".bp-" + userID.getValue();
            getFilesystem().setFilesystemRootBucketName(new FilesystemRootBucketName(root));
        }
        if (getAmazons3() != null) {
            String root = getAmazons3().getAmazonS3RootBucketName().getValue();
            root += ".bp-" + userID.getValue();
            getAmazons3().setAmazonS3RootBucketName(new AmazonS3RootBucketName(root));
        }
    }

    public ConnectionProperties getProperties() {
        if (filesystem != null) {
            return filesystem;
        }
        return amazons3;
    }
}

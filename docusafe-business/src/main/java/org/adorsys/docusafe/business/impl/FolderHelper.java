package org.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.complextypes.BucketDirectory;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import org.adorsys.docusafe.service.api.keystore.types.KeyStoreType;
import org.adorsys.docusafe.service.api.types.UserID;

public class FolderHelper {
    public static BucketDirectory getRootDirectory(UserID userID) {
        return new BucketDirectory("bp-"+userID.getValue());
    }

    public static BucketPath getKeyStorePath(UserID userID) {
        return getRootDirectory(userID).appendName("keystore." + KeyStoreType.DEFAULT.getValue());
    }

    public static BucketPath getDFSCredentialsPath(UserID userID) {
        return getRootDirectory(userID).appendName("UserDFSCredentials");
    }

    public static BucketPath getPublicKeyListPath(UserID userID) {
        return getRootDirectory(userID).appendName("publicKeys");
    }
}

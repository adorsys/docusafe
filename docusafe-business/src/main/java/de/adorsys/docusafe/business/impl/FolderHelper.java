package de.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.complextypes.BucketDirectory;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.docusafe.service.api.keystore.types.KeyStoreType;
import de.adorsys.docusafe.service.api.types.UserID;

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

    public static BucketDirectory getHomeDirectory(UserID userID) {
        return getRootDirectory(userID).appendDirectory("home");
    }

    public static BucketDirectory getInboxDirectory(UserID userID) {
        return getRootDirectory(userID).appendDirectory("inbox");
    }
}

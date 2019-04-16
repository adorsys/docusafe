package org.adorsys.docusafe.service.api.bucketpathencryption;


import de.adorsys.dfs.connection.api.complextypes.BucketDirectory;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public interface BucketPathEncryptionService {

    BucketPath encrypt(SecretKey secretKey, BucketPath bucketPath);

    BucketPath decrypt(SecretKey secretKey, BucketPath bucketPath);

    BucketDirectory encrypt(SecretKey secretKey, BucketDirectory bucketDirectory);

    BucketDirectory decrypt(SecretKey secretKey, BucketDirectory bucketDirectory);
}

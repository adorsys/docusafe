package de.adorsys.docusafe.transactional.versioning;

import de.adorsys.dfs.connection.api.complextypes.BucketPath;

import java.util.List;

public interface VersioningService {
    VersionedBucketPath getNewVersionForBucketPath(BucketPath unversionedBucketPath);
    VersionedBucketPath getLatestVersionForBucketPath(BucketPath unversionedBucketPath);
    List<VersionedBucketPath> getAllVersionsForBucketPath(BucketPath unversionedBucketPath);
    void deleteAllVersionsBeforeVersionedBucketPath(VersionedBucketPath versionedBucketPath);
}

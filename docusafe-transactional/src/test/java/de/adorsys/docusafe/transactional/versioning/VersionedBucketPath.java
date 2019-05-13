package de.adorsys.docusafe.transactional.versioning;

import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class VersionedBucketPath extends BucketPath {

    UUID version;

    public VersionedBucketPath(String path) {
        super(path);
    }

    public VersionedBucketPath(BucketPath bucketPath) {
        super(bucketPath);
    }
}

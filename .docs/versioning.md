# Versioning

To make the same file writable in different versions each file gets a suffix. This suffix is a time ordered UUID.
So a ```BucketPath``` and a ```UUID``` are assumed to be a ```VersionedBucketPath```. 

If versioning is used, each file command (read/write/delete/exists) works as before, but gets a ```VersionedBucketPath```
rather than a ```BucketPath```. 

The interface for a versioning service should look like this:

```
public interface VersioningService {
    VersionedBucketPath getNewVersionForBucketPath(BucketPath unversionedBucketPath);
    VersionedBucketPath getLatestVersionForBucketPath(BucketPath unversionedBucketPath);
    List<VersionedBucketPath> getAllVersionsForBucketPath(BucketPath unversionedBucketPath);
    void deleteAllVersionsBeforeVersionedBucketPath(VersionedBucketPath versionedBucketPath);
}
```

To create a new Version a time ordered UUID is to be created. A random UUID does not work! Creating a time ordered unique UUID can be done with:  

```
                // uuid = UUID.randomUUID();
                uuid = com.datastax.driver.core.utils.UUIDs.timeBased();
```

The finding of the latest version is an expensive command. It actually has to go to the DFS and ask for all files with the same prefix. Than the prefixes just have to be sorted (as Strings). Than the first UUID is the oldest, and the last the newest.

The write command is cheap, because just a new VersionedBucketPath is to be created.
But every read command is expensive as for that the latest (or last) Version has to be found.

#
A test proving that the time ordered UUID is 

0. time based ordered and 

0. unique

can be found here ![ParallelUUIDTest](../docusafe-transactional/src/test/java/de/adorsys/docusafe/transactional/ParallelUUIDTest.java)


# 
The dependency for the datastax.driver is:
```
        <dependency>
            <groupId>com.datastax.cassandra</groupId>
            <artifactId>cassandra-driver-core</artifactId>
            <version>1.0.8</version>
            <scope>test</scope>
        </dependency>
``` 



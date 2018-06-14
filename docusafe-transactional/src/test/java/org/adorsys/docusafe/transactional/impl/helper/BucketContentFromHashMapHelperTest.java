package org.adorsys.docusafe.transactional.impl.helper;

import org.adorsys.docusafe.business.types.complex.BucketContentFQN;
import org.adorsys.docusafe.business.types.complex.DocumentDirectoryFQN;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.encobject.types.ListRecursiveFlag;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by peter on 14.06.18 at 18:23.
 */
@SuppressWarnings("Duplicates")
public class BucketContentFromHashMapHelperTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(BucketContentFromHashMapHelperTest.class);
    Set<DocumentFQN> keys = new HashSet<>();


    @Before
    public void init() {
        keys.add(new DocumentFQN("/a/file1"));
        keys.add(new DocumentFQN("/a/file2"));
        keys.add(new DocumentFQN("/a/b/c/file1"));
        keys.add(new DocumentFQN("/a/b/c/file2"));
        keys.add(new DocumentFQN("/a/b/c/file3"));
        keys.add(new DocumentFQN("/a/b/c/file4"));
    }

    @Test
    public void testRecursiveWithFolder() {
        BucketContentFQN a = BucketContentFromHashMapHelper.list(keys, new DocumentDirectoryFQN("a"), ListRecursiveFlag.TRUE);
        a.getFiles().forEach(file -> LOGGER.debug("file: " + file));
        a.getDirectories().forEach(dir -> LOGGER.debug("dir: " + dir));
        Assert.assertEquals(6, a.getFiles().size());
        Assert.assertEquals(2, a.getDirectories().size());
    }

    @Test
    public void testRecursiveWithRoot() {
        BucketContentFQN a = BucketContentFromHashMapHelper.list(keys, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE);
        a.getFiles().forEach(file -> LOGGER.debug("file: " + file));
        a.getDirectories().forEach(dir -> LOGGER.debug("dir: " + dir));
        Assert.assertEquals(6, a.getFiles().size());
        Assert.assertEquals(3, a.getDirectories().size());
    }

    @Test
    public void testRecursiveWithNonExsistantFolder() {
        BucketContentFQN a = BucketContentFromHashMapHelper.list(keys, new DocumentDirectoryFQN("/b"), ListRecursiveFlag.TRUE);
        a.getFiles().forEach(file -> LOGGER.debug("file: " + file));
        a.getDirectories().forEach(dir -> LOGGER.debug("dir: " + dir));
        Assert.assertEquals(0, a.getFiles().size());
        Assert.assertEquals(0, a.getDirectories().size());
    }

    @Test
    public void testNonRecursiveWithFolder() {
        BucketContentFQN a = BucketContentFromHashMapHelper.list(keys, new DocumentDirectoryFQN("a"), ListRecursiveFlag.FALSE);
        a.getFiles().forEach(file -> LOGGER.debug("file: " + file));
        a.getDirectories().forEach(dir -> LOGGER.debug("dir: " + dir));
        Assert.assertEquals(2, a.getFiles().size());
        Assert.assertEquals(1, a.getDirectories().size());
    }

    @Test
    public void testNonRecursiveWithRoot() {
        BucketContentFQN a = BucketContentFromHashMapHelper.list(keys, new DocumentDirectoryFQN("/"), ListRecursiveFlag.FALSE);
        a.getFiles().forEach(file -> LOGGER.debug("file: " + file));
        a.getDirectories().forEach(dir -> LOGGER.debug("dir: " + dir));
        Assert.assertEquals(0, a.getFiles().size());
        Assert.assertEquals(1, a.getDirectories().size());
    }

    @Test
    public void testNonRecursiveWithNonExsistantFolder() {
        BucketContentFQN a = BucketContentFromHashMapHelper.list(keys, new DocumentDirectoryFQN("/b"), ListRecursiveFlag.FALSE);
        a.getFiles().forEach(file -> LOGGER.debug("file: " + file));
        a.getDirectories().forEach(dir -> LOGGER.debug("dir: " + dir));
        Assert.assertEquals(0, a.getFiles().size());
        Assert.assertEquals(0, a.getDirectories().size());
    }

}

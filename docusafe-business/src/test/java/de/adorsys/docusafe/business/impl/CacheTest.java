package de.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.impl.caches.UsersPrivateKeyStoreCache;
import de.adorsys.docusafe.business.impl.caches.guava.UserDFSCredentialsCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.caches.guava.UserPathSecretKeyCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.caches.guava.UserPublicKeyListCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.caches.guava.UsersPrivateKeyStoreCacheGuavaImpl;
import de.adorsys.docusafe.business.impl.dummyCaches.UserDFSCredentialsCacheNoCacheTestImpl;
import de.adorsys.docusafe.business.impl.dummyCaches.UserPathSecretKeyCacheNoCacheTestImpl;
import de.adorsys.docusafe.business.impl.dummyCaches.UserPublicKeyListCacheNoCacheTestImpl;
import de.adorsys.docusafe.business.impl.dummyCaches.UsersPrivateKeyStoreCacheNoCacheTestImpl;
import de.adorsys.docusafe.business.types.DFSCredentials;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RunWith(value = PowerMockRunner.class)
@PrepareForTest({
        DFSConnectionFactory.class,
        DFSConnection.class,
        DocumentSafeServiceImpl.class,

        UsersPrivateKeyStoreCache.class,
        UsersPrivateKeyStoreCacheGuavaImpl.class,
        UserPublicKeyListCacheGuavaImpl.class,
        UserPathSecretKeyCacheGuavaImpl.class,
        UserDFSCredentialsCacheGuavaImpl.class})
@PowerMockIgnore("javax.*")
public class CacheTest {

    @SneakyThrows
    @Test
    public void testWithoutCache() {
        PowerMockito.whenNew(UsersPrivateKeyStoreCacheGuavaImpl.class).withNoArguments().thenAnswer(in -> {
            log.info("powermodckit works fine for UsersPrivateKeyStoreCacheGuavaImpl");
            return new UsersPrivateKeyStoreCacheNoCacheTestImpl();
        });

        PowerMockito.whenNew(UserPublicKeyListCacheGuavaImpl.class).withNoArguments().thenAnswer(in -> {
            log.info("powermodckit works fine for UserPublicKeyListCacheGuavaImpl");
            return new UserPublicKeyListCacheNoCacheTestImpl();
        });

        PowerMockito.whenNew(UserPathSecretKeyCacheGuavaImpl.class).withNoArguments().thenAnswer(in -> {
            log.info("powermodckit works fine for UserPathSecretKeyCacheGuavaImpl");
            return new UserPathSecretKeyCacheNoCacheTestImpl();
        });

        PowerMockito.whenNew(UserDFSCredentialsCacheGuavaImpl.class).withNoArguments().thenAnswer(in -> {
            log.info("powermodckit works fine for UserDFSCredentialsCacheGuavaImpl");
            return new UserDFSCredentialsCacheNoCacheTestImpl();
        });

        DFSConnectionFactory dfsConnectionFactory = Mockito.spy(new DFSConnectionFactory());
        PowerMockito.whenNew(DFSConnectionFactory.class).withAnyArguments().thenAnswer(in -> {
            log.info("powermodckit works fine fore connection factory");
            return dfsConnectionFactory;
        });

        /* getDFSConnection is called several times
         * 1) during creation of service
         * 2) internaly a new system dfs connection with another root bucket is created
         * 3)           and a new user dfs with another root bucket
         * 4) for write call the user dfs is created after the credentials have been read from the system dfs
         *
         * as the system dfs is reused all the times, some tricks are used,
         * to not reshow values from the previous action
         */
        ResultCaptor<DFSConnection> dfss = new ResultCaptor<>();
        Mockito.doAnswer(dfss).when(dfsConnectionFactory).getDFSConnection(Mockito.any());

        DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));

        int SYSTEM_DFS_INDEX = 1;
        int USERS_DFS_INDEX = 2;
        int system_dfs_read_last = 0;
        int system_dfs_wrote_last = 0;
        try {
            {
                // FIRST THE USER IS CREATED
                service.createUser(userIDAuth);
                // show("user is created", dfss.getResults());
                Assert.assertEquals(3, dfss.getResults().size());

                {
                    // CREATE USER writes the following documentes to the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Assert.assertEquals(3, dfss.getResults().size());
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_wrote_last + 3)).putBlob(captureBucketPath.capture(), Mockito.any());
                    for (int i = system_dfs_wrote_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("CREATE USER written to the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(system_dfs_wrote_last + 3, captureBucketPath.getAllValues().size());
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_wrote_last + 0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_wrote_last + 1).getValue(), "bp-peter/UserDFSCredentials");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_wrote_last + 2).getValue(), "bp-peter/publicKeys");
                    system_dfs_wrote_last += 3;
                }
                {
                    // CREATE USER does not read documentes from the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_read_last + 0)).getBlob(captureBucketPath.capture());
                    for (int i = system_dfs_read_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("CREATE USER read from the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(system_dfs_read_last + 0, captureBucketPath.getAllValues().size());
                    system_dfs_read_last += 0;
                }
                {
                    // CREATE USER writes the following documentes to the users dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(1)).putBlob(captureBucketPath.capture(), Mockito.any());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER written to the users dfs:" + bp));
                    Assert.assertEquals(1, captureBucketPath.getAllValues().size());
                    Assert.assertEquals(captureBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                }
                {
                    // CREATE USER does not read documentes from the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(0)).getBlob(captureBucketPath.capture());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER read from the users dfs:" + bp));
                    Assert.assertTrue(captureBucketPath.getAllValues().isEmpty());
                }
            }

            DSDocument dsDocument = null;
            {
                // NOW A DOCUMENT WILL BE WRITTEN
                DocumentFQN documentFQN = new DocumentFQN("file1.txt");
                DocumentContent documentContent = new DocumentContent("affe".getBytes());
                dsDocument = new DSDocument(documentFQN, documentContent);
                service.storeDocument(userIDAuth, dsDocument);
                Assert.assertEquals(4, dfss.getResults().size());
                USERS_DFS_INDEX++;

                // WRITE DOCUMENT writes no documentes to the system dfs
                {
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_wrote_last + 0)).putBlob(captureBucketPath.capture(), Mockito.any());
                    for (int i = system_dfs_wrote_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("WRITE DOCUMENT written to the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(system_dfs_wrote_last + 0, captureBucketPath.getAllValues().size());
                    system_dfs_wrote_last += 0;
                }
                {
                    // WRITE DOCUMENT reads the following documents from the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_read_last + 3)).getBlob(captureBucketPath.capture());
                    for (int i = system_dfs_read_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("WRITE DOCUMENT read from the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_read_last + 0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_read_last + 1).getValue(), "bp-peter/UserDFSCredentials");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_read_last + 2).getValue(), "bp-peter/publicKeys");
                    Assert.assertEquals(system_dfs_read_last + 3, captureBucketPath.getAllValues().size());
                    system_dfs_read_last += 3;
                }
                {
                    // WRITE DOCUMENT writes the documents itself to the users dfs, as the name ie encrypted, it is not captured
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(1)).putBlob(captureBucketPath.capture(), Mockito.any());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("WRITE DOCUMENT written to the users dfs:" + bp));
                    Assert.assertEquals(1, captureBucketPath.getAllValues().size());
                }
                {
                    // WRITE DOCUMENT reads the following documents from the users dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(1)).getBlob(captureBucketPath.capture());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("WRITE DOCUMENT read from the users dfs:" + bp));
                    Assert.assertEquals(captureBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(1, captureBucketPath.getAllValues().size());
                }
            }

            {
                // NOW A DOCUMENT WILL BE READ

                DSDocument dsDocument1 = service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
                Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());

                Assert.assertEquals(5, dfss.getResults().size());
                USERS_DFS_INDEX++;
                {
                    // WRITE DOCUMENT writes no documentes to the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_wrote_last + 0)).putBlob(captureBucketPath.capture(), Mockito.any());
                    for (int i = system_dfs_wrote_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("READ DOCUMENT written to the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(system_dfs_wrote_last + 0, captureBucketPath.getAllValues().size());
                    system_dfs_wrote_last += 0;
                }
                {
                    // WRITE DOCUMENT reads two documentes from the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_read_last + 2)).getBlob(captureBucketPath.capture());
                    for (int i = system_dfs_read_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("READ DOCUMENT read from the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(system_dfs_read_last + 2, captureBucketPath.getAllValues().size());
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_read_last + 0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_read_last + 1).getValue(), "bp-peter/UserDFSCredentials");
                    system_dfs_read_last += 2;
                }
                {
                    // READ DOCUMENT writes no documents to the users dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(0)).putBlob(captureBucketPath.capture(), Mockito.any());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("READ DOCUMENT written to the users dfs:" + bp));
                    Assert.assertTrue(captureBucketPath.getAllValues().isEmpty());
                }
                {
                    // READ DOCUMENT reads the following documents from the users dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(2)).getBlob(captureBucketPath.capture());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("READ DOCUMENT read from the users dfs:" + bp));
                    Assert.assertEquals(captureBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                }
            }

        } finally {
            log.info("start destroying user");
            // show("before destroying user", dfss.getResults());


            service.destroyUser(userIDAuth);
            log.info("finished");
            // show("after destroying user", dfss.getResults());
        }

    }

    @SneakyThrows
    @Test
    public void testWithCache() {
        DFSConnectionFactory dfsConnectionFactory = Mockito.spy(new DFSConnectionFactory());
        PowerMockito.whenNew(DFSConnectionFactory.class).withAnyArguments().thenAnswer(in -> {
            log.info("powermodckit works fine fore connection factory");
            return dfsConnectionFactory;
        });

        /* getDFSConnection is called several times
         * 1) during creation of service
         * 2) internaly a new system dfs connection with another root bucket is created
         * 3)           and a new user dfs with another root bucket
         * 4) for write call the user dfs is created after the credentials have been read from the system dfs
         *
         * as the system dfs is reused all the times, some tricks are used,
         * to not reshow values from the previous action
         */
        ResultCaptor<DFSConnection> dfss = new ResultCaptor<>();
        Mockito.doAnswer(dfss).when(dfsConnectionFactory).getDFSConnection(Mockito.any());

        DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));

        int SYSTEM_DFS_INDEX = 1;
        int USERS_DFS_INDEX = 2;
        int system_dfs_read_last = 0;
        int system_dfs_wrote_last = 0;
        try {
            {
                // FIRST THE USER IS CREATED
                service.createUser(userIDAuth);
                // show("user is created", dfss.getResults());
                Assert.assertEquals(3, dfss.getResults().size());

                {
                    // CREATE USER writes the following documentes to the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Assert.assertEquals(3, dfss.getResults().size());
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_wrote_last + 3)).putBlob(captureBucketPath.capture(), Mockito.any());
                    for (int i = system_dfs_wrote_last; i < captureBucketPath.getAllValues().size(); i++) {
                        log.info("CREATE USER written to the system dfs:" + captureBucketPath.getAllValues().get(i));
                    }
                    Assert.assertEquals(system_dfs_wrote_last + 3, captureBucketPath.getAllValues().size());
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_wrote_last + 0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_wrote_last + 1).getValue(), "bp-peter/UserDFSCredentials");
                    Assert.assertEquals(captureBucketPath.getAllValues().get(system_dfs_wrote_last + 2).getValue(), "bp-peter/publicKeys");
                    system_dfs_wrote_last += 3;
                }
                {
                    // CREATE USER does not read documentes from the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_read_last + 0)).getBlob(captureBucketPath.capture());
                    Assert.assertEquals(system_dfs_read_last + 0, captureBucketPath.getAllValues().size());
                    system_dfs_read_last += 0;
                }
                {
                    // CREATE USER writes the following documentes to the users dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(1)).putBlob(captureBucketPath.capture(), Mockito.any());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER written to the users dfs:" + bp));
                    Assert.assertEquals(1, captureBucketPath.getAllValues().size());
                    Assert.assertEquals(captureBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                }
                {
                    // CREATE USER does not read documentes from the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(0)).getBlob(captureBucketPath.capture());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER read from the users dfs:" + bp));
                    Assert.assertTrue(captureBucketPath.getAllValues().isEmpty());
                }
            }

            DSDocument dsDocument = null;
            {
                // NOW A DOCUMENT WILL BE WRITTEN
                DocumentFQN documentFQN = new DocumentFQN("file1.txt");
                DocumentContent documentContent = new DocumentContent("affe".getBytes());
                dsDocument = new DSDocument(documentFQN, documentContent);
                service.storeDocument(userIDAuth, dsDocument);
                Assert.assertEquals(4, dfss.getResults().size());
                USERS_DFS_INDEX++;

                // WRITE DOCUMENT writes no documentes to the system dfs
                {
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_wrote_last + 0)).putBlob(captureBucketPath.capture(), Mockito.any());
                    Assert.assertEquals(system_dfs_wrote_last + 0, captureBucketPath.getAllValues().size());
                }
                {
                    // WRITE DOCUMENT reads no documents from the system dfs as everything is cached
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_read_last + 0)).getBlob(captureBucketPath.capture());
                    Assert.assertEquals(system_dfs_read_last + 0, captureBucketPath.getAllValues().size());
                }
                {
                    // WRITE DOCUMENT writes the documents itself to the users dfs, as the name ie encrypted, it is not captured
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(1)).putBlob(captureBucketPath.capture(), Mockito.any());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("WRITE DOCUMENT written to the users dfs:" + bp));
                    Assert.assertEquals(1, captureBucketPath.getAllValues().size());
                }
                {
                    // WRITE DOCUMENT reads no documents from the users dfs as everything is cached
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(0)).getBlob(captureBucketPath.capture());
                    Assert.assertEquals(0, captureBucketPath.getAllValues().size());
                }
            }

            {
                // NOW A DOCUMENT WILL BE READ

                DSDocument dsDocument1 = service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
                Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());

                Assert.assertEquals(5, dfss.getResults().size());
                USERS_DFS_INDEX++;
                {
                    // WRITE DOCUMENT writes no documentes to the system dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_wrote_last + 0)).putBlob(captureBucketPath.capture(), Mockito.any());
                    Assert.assertEquals(system_dfs_wrote_last + 0, captureBucketPath.getAllValues().size());
                }
                {
                    // WRITE DOCUMENT reads 0 documentes from the system dfs as everything is cached
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(SYSTEM_DFS_INDEX), Mockito.times(system_dfs_read_last + 0)).getBlob(captureBucketPath.capture());
                    Assert.assertEquals(system_dfs_read_last + 0, captureBucketPath.getAllValues().size());
                }
                {
                    // READ DOCUMENT writes no documents to the users dfs
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(0)).putBlob(captureBucketPath.capture(), Mockito.any());
                    Assert.assertTrue(captureBucketPath.getAllValues().isEmpty());
                }
                {
                    // READ DOCUMENT reads the ondocuments from the users dfs, no check for name as name is encrypted
                    ArgumentCaptor<BucketPath> captureBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    Mockito.verify(dfss.getResults().get(USERS_DFS_INDEX), Mockito.times(1)).getBlob(captureBucketPath.capture());
                    captureBucketPath.getAllValues().forEach(bp -> log.info("READ DOCUMENT read from the users dfs:" + bp));
                    Assert.assertEquals(system_dfs_read_last + 1, captureBucketPath.getAllValues().size());
                }
            }

        } finally {
            log.info("start destroying user");
            // show("before destroying user", dfss.getResults());


            service.destroyUser(userIDAuth);
            log.info("finished");
            // show("after destroying user", dfss.getResults());
        }

    }

    private void show(String message, List<DFSConnection> results) {
        log.info(message);
        int i = 0;
        for (DFSConnection c : results) {
            log.info(i++ + " dfs connection " + new DFSCredentials(c.getConnectionProperties()).toString());
        }
        ;
    }


    public class ResultCaptor<T> implements Answer {
        private List<DFSConnection> results = new ArrayList<>();

        public List<DFSConnection> getResults() {
            return results;
        }

        @Override
        public T answer(InvocationOnMock invocationOnMock) throws Throwable {
            log.info("a new dfs connection");
            T t = Mockito.spy((T) invocationOnMock.callRealMethod());
            results.add((DFSConnection) t);
            log.info("size is now " + results.size());
            return t;
        }
    }
}

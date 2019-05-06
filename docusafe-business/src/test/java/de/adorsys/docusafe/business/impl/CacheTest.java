package de.adorsys.docusafe.business.impl;

import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.dfs.connection.api.service.api.DFSConnection;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
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
@PrepareForTest({DFSConnectionFactory.class, DFSConnection.class})
@PowerMockIgnore("javax.*")
public class CacheTest {

    @SneakyThrows
    @Test
    public void createUserWithCache() {
        DFSConnectionFactory dfsConnectionFactory = Mockito.spy(new DFSConnectionFactory());
        PowerMockito.whenNew(DFSConnectionFactory.class).withAnyArguments().thenAnswer(in -> {
            log.info("powermodckit works fine");
            return dfsConnectionFactory;
        });

        /* getDFSConnection is called several times
         * 1) during creation of service
         * 2) internaly a new system dfs connection with another root bucket is created
         * 3)           and a new user dfs with another root bucket
         * 4) for write call the user dfs is created after the credentials have been read from the system dfs
         *
         */
        ResultCaptor<DFSConnection> dfss = new ResultCaptor<>();
        Mockito.doAnswer(dfss).when(dfsConnectionFactory).getDFSConnection(Mockito.any());

        DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));

        try {
            // FIRST THE USER IS CREATED
            {
                {
                    ArgumentCaptor<BucketPath> writeBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    ArgumentCaptor<BucketPath> readBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    service.createUser(userIDAuth);
                    show("user is created", dfss.getResults());
                    Assert.assertEquals(3, dfss.getResults().size());

                    // CREATE USER writes the following documentes to the system dfs
                    Assert.assertEquals(3, dfss.getResults().size());
                    Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).putBlob(writeBucketPath.capture(), Mockito.any());
                    writeBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER written to the system dfs:" + bp));
                    Assert.assertEquals(3, writeBucketPath.getAllValues().size());
                    Assert.assertEquals(writeBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(writeBucketPath.getAllValues().get(1).getValue(), "bp-peter/UserDFSCredentials");
                    Assert.assertEquals(writeBucketPath.getAllValues().get(2).getValue(), "bp-peter/publicKeys");

                    // CREATE USER does not read documentes from the system dfs
                    Mockito.verify(dfss.getResults().get(1), Mockito.times(0)).getBlob(readBucketPath.capture());
                    readBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER read from the system dfs:" + bp));
                    Assert.assertTrue(readBucketPath.getAllValues().isEmpty());
                }
                {
                    ArgumentCaptor<BucketPath> writeBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    ArgumentCaptor<BucketPath> readBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    // CREATE USER writes the following documentes to the users dfs
                    Mockito.verify(dfss.getResults().get(2), Mockito.times(1)).putBlob(writeBucketPath.capture(), Mockito.any());
                    writeBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER written to the users dfs:" + bp));
                    Assert.assertEquals(1, writeBucketPath.getAllValues().size());
                    Assert.assertEquals(writeBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");

                    // CREATE USER does not read documentes from the system dfs
                    Mockito.verify(dfss.getResults().get(2), Mockito.times(0)).getBlob(readBucketPath.capture());
                    readBucketPath.getAllValues().forEach(bp -> log.info("CREATE USER read from the users dfs:" + bp));
                    Assert.assertTrue(readBucketPath.getAllValues().isEmpty());
                }
            }

            DSDocument dsDocument = null;
            // NOW A DOCUMENT WILL BE WRITTEN
            {
                {
                    ArgumentCaptor<BucketPath> readBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    {
                        DocumentFQN documentFQN = new DocumentFQN("file1.txt");
                        DocumentContent documentContent = new DocumentContent("affe".getBytes());
                        dsDocument = new DSDocument(documentFQN, documentContent);
                        service.storeDocument(userIDAuth, dsDocument);
                    }
                    Assert.assertEquals(4, dfss.getResults().size());

                    // WRITE DOCUMENT writes no documentes to the system dfs, so it is STILL 3 (from the first time)
                    Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).putBlob(Mockito.any(), Mockito.any());

                    // WRITE DOCUMENT reads the following documents from the system dfs
                    Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).getBlob(readBucketPath.capture());
                    readBucketPath.getAllValues().forEach(bp -> log.info("WRITE DOCUMENT read from the system dfs:" + bp));
                    Assert.assertEquals(3, readBucketPath.getAllValues().size());
                    Assert.assertEquals(readBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(readBucketPath.getAllValues().get(1).getValue(), "bp-peter/UserDFSCredentials");
                    Assert.assertEquals(readBucketPath.getAllValues().get(2).getValue(), "bp-peter/publicKeys");
                }
                {
                    ArgumentCaptor<BucketPath> writeBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    ArgumentCaptor<BucketPath> readBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    // WRITE DOCUMENT writes the documents itself to the users dfs, as the name ie encrypted, it is not captured
                    Mockito.verify(dfss.getResults().get(3), Mockito.times(1)).putBlob(writeBucketPath.capture(), Mockito.any());
                    writeBucketPath.getAllValues().forEach(bp -> log.info("WRITE DOCUMENT written to the users dfs:" + bp));
                    Assert.assertEquals(1, writeBucketPath.getAllValues().size());


                    // WRITE DOCUMENT reads the following documents from the users dfs
                    Mockito.verify(dfss.getResults().get(3), Mockito.times(1)).getBlob(readBucketPath.capture());
                    readBucketPath.getAllValues().forEach(bp -> log.info("WRITE DOCUMENT read from the users dfs:" + bp));
                    Assert.assertEquals(readBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                }
            }

            // NOW A DOCUMENT WILL BE READ
            {
                {
                    ArgumentCaptor<BucketPath> readBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    DSDocument dsDocument1 = service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
                    Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());

                    Assert.assertEquals(5, dfss.getResults().size());

                    // WRITE DOCUMENT writes no documentes to the system dfs, so it is STILL 3 (from the first time)
                    Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).putBlob(Mockito.any(), Mockito.any());

                    // READ DOCUMENT reads the following documents from the system dfs (3 from the previous and now another 2)
                    Mockito.verify(dfss.getResults().get(1), Mockito.times(5)).getBlob(readBucketPath.capture());
                    readBucketPath.getAllValues().forEach(bp -> log.info("READ DOCUMENT read from the system dfs:" + bp));
                    Assert.assertEquals(5, readBucketPath.getAllValues().size());
                    Assert.assertEquals(readBucketPath.getAllValues().get(3).getValue(), "bp-peter/keystore.UBER");
                    Assert.assertEquals(readBucketPath.getAllValues().get(4).getValue(), "bp-peter/UserDFSCredentials");
                }
                {
                    ArgumentCaptor<BucketPath> writeBucketPath = ArgumentCaptor.forClass(BucketPath.class);
                    ArgumentCaptor<BucketPath> readBucketPath = ArgumentCaptor.forClass(BucketPath.class);

                    // READ DOCUMENT writes no documents to the users dfs
                    Mockito.verify(dfss.getResults().get(4), Mockito.times(0)).putBlob(writeBucketPath.capture(), Mockito.any());
                    writeBucketPath.getAllValues().forEach(bp -> log.info("READ DOCUMENT written to the users dfs:" + bp));
                    Assert.assertTrue(writeBucketPath.getAllValues().isEmpty());

                    // READ DOCUMENT reads the following documents from the users dfs
                    Mockito.verify(dfss.getResults().get(4), Mockito.times(2)).getBlob(readBucketPath.capture());
                    readBucketPath.getAllValues().forEach(bp -> log.info("READ DOCUMENT read from the users dfs:" + bp));
                    Assert.assertEquals(readBucketPath.getAllValues().get(0).getValue(), "bp-peter/keystore.UBER");
                }
            }

        } finally {
            log.info("start destroying user");
            show("before destroying user", dfss.getResults());


            service.destroyUser(userIDAuth);
            log.info("finished");
            show("after destroying user", dfss.getResults());
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

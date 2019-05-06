package de.adorsys.docusafe.business.impl;

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
        // ArgumentCaptor<ConnectionProperties> props = ArgumentCaptor.forClass(ConnectionProperties.class);

        ResultCaptor<DFSConnection> dfss = new ResultCaptor<>();
        Mockito.doAnswer(dfss).when(dfsConnectionFactory).getDFSConnection(Mockito.any());

        DocumentSafeService service = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("affe"));

        try {
            service.createUser(userIDAuth);
            show(dfss.getResults());

            Assert.assertEquals(3, dfss.getResults().size());
            // system users keystore
            // system users dfs credentials
            // system users public keys
            Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).putBlob(Mockito.any(), Mockito.any());
            Mockito.verify(dfss.getResults().get(1), Mockito.times(0)).getBlob(Mockito.any());

            // users keystore has been written
            Mockito.verify(dfss.getResults().get(2), Mockito.times(1)).putBlob(Mockito.any(), Mockito.any());
            Mockito.verify(dfss.getResults().get(2), Mockito.times(0)).getBlob(Mockito.any());

            DSDocument dsDocument = null;
            {
                DocumentFQN documentFQN = new DocumentFQN("file1.txt");
                DocumentContent documentContent = new DocumentContent("affe".getBytes());
                dsDocument = new DSDocument(documentFQN, documentContent);
                service.storeDocument(userIDAuth, dsDocument);
            }
            Assert.assertEquals(4, dfss.getResults().size());
            // nothing to be written
            Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).putBlob(Mockito.any(), Mockito.any());
            // get system users keystore (for path encoding)
            // get system users dfs credentials
            // get system users public keys for documenet encoding
            Mockito.verify(dfss.getResults().get(1), Mockito.times(3)).getBlob(Mockito.any());

            // document written
            Mockito.verify(dfss.getResults().get(3), Mockito.times(1)).putBlob(Mockito.any(), Mockito.any());
            // get users keystore (for path encoding)
            Mockito.verify(dfss.getResults().get(3), Mockito.times(1)).getBlob(Mockito.any());

            {
                DSDocument dsDocument1 = service.readDocument(userIDAuth, dsDocument.getDocumentFQN());
                Assert.assertArrayEquals(dsDocument.getDocumentContent().getValue(), dsDocument1.getDocumentContent().getValue());
            }


        } finally {
            log.info("start destroying user");
            show(dfss.getResults());


            service.destroyUser(userIDAuth);
            log.info("finished");
            show(dfss.getResults());
        }

    }

    private void show(List<DFSConnection> results) {
        int i = 0;
        for (DFSConnection c:results) {
            log.info(i++ + " dfs connection " + new DFSCredentials(c.getConnectionProperties()).toString());
        };
    }


    public class ResultCaptor<T> implements Answer {
        private List<DFSConnection> results = new ArrayList<>();

        public List<DFSConnection> getResults() {
            return results;
        }

        @Override
        public T answer(InvocationOnMock invocationOnMock) throws Throwable {
            log.info("a new dfs connection");
            T t = Mockito.spy( (T) invocationOnMock.callRealMethod());
            results.add((DFSConnection) t);
            log.info("size is now " + results.size());
            return t;
        }
    }
}

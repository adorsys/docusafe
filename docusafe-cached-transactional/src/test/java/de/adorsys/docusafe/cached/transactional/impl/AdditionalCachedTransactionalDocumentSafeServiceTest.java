package de.adorsys.docusafe.cached.transactional.impl;

import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.DocumentContent;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.business.types.BucketContentFQN;
import de.adorsys.docusafe.business.types.DSDocument;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.RequestMemoryContext;
import de.adorsys.docusafe.transactional.TransactionalDocumentSafeService;
import de.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import de.adorsys.docusafe.transactional.types.TxBucketContentFQN;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 09.07.18 at 13:52.
 */

public class AdditionalCachedTransactionalDocumentSafeServiceTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(AdditionalCachedTransactionalDocumentSafeServiceTest.class);
    private RequestMemoryContext memoryContext = new SimpleRequestMemoryContextImpl();
    private DocumentSafeService dss = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
    private TransactionalDocumentSafeServiceTestWrapper wrapper = new TransactionalDocumentSafeServiceTestWrapper(new TransactionalDocumentSafeServiceImpl(memoryContext, dss));
    private TransactionalDocumentSafeService cachedService = new CachedTransactionalDocumentSafeServiceImpl(memoryContext, wrapper, dss );
    private List<UserIDAuth> userIDAuthList = new ArrayList<>();


    @Before
    public void before() {
        userIDAuthList.clear();
    }

    @After
    public void after() {
        userIDAuthList.forEach(userIDAuth -> cachedService.destroyUser(userIDAuth));
        LOGGER.debug("aftertest:" + wrapper.toString());
    }

    @Test
    public void testTxListAndDeleteDocument() {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("petersPassword"));
        cachedService.createUser(userIDAuth);
        userIDAuthList.add(userIDAuth);

        DocumentFQN documentFQN = new DocumentFQN("folder1/file1.txt");
        cachedService.beginTransaction(userIDAuth);
        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_LIST_DOCUMENTS));
        BucketContentFQN bucketContentFQN = cachedService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
        Assert.assertTrue(bucketContentFQN.getFiles().isEmpty());
        Assert.assertFalse(cachedService.txDocumentExists(userIDAuth, documentFQN));

        // document speichern
        {
            DSDocument dsDocumentWrite = new DSDocument(
                    documentFQN,
                    new DocumentContent("content of file".getBytes())
            );
            cachedService.txStoreDocument(userIDAuth, dsDocumentWrite);
            DSDocument dsDocumentRead = cachedService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertArrayEquals(dsDocumentWrite.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue());
        }
        BucketContentFQN bucketContentFQN2 = cachedService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
        Assert.assertEquals(1, bucketContentFQN2.getFiles().size());
        Assert.assertTrue(cachedService.txDocumentExists(userIDAuth, documentFQN));

        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_STORE_DOCUMENT));
        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_READ_DOCUMENT));
        Assert.assertEquals(new Integer(1), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_LIST_DOCUMENTS));
        cachedService.endTransaction(userIDAuth);
        LOGGER.debug(cachedService.toString());
        Assert.assertEquals(new Integer(1), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_STORE_DOCUMENT));
        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_READ_DOCUMENT));
        Assert.assertEquals(new Integer(1), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_LIST_DOCUMENTS));
        Assert.assertEquals(documentFQN, bucketContentFQN2.getFiles().get(0));
    }

    @Test
    public void testTxReadAndStore() {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("petersPassword"));
        DocumentFQN documentFQN = new DocumentFQN("folder1/file1.txt");
        cachedService.createUser(userIDAuth);
        userIDAuthList.add(userIDAuth);
        cachedService.beginTransaction(userIDAuth);

        // document speichern
        {
            DSDocument dsDocumentWrite = new DSDocument(
                    documentFQN,
                    new DocumentContent("content of file".getBytes())
            );
            cachedService.txStoreDocument(userIDAuth, dsDocumentWrite);
            DSDocument dsDocumentRead = cachedService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertArrayEquals(dsDocumentWrite.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue());
        }

        // Document überschreiben
        {
            DSDocument dsDocumentWrite = new DSDocument(
                    documentFQN,
                    new DocumentContent("another content of file".getBytes())
            );
            cachedService.txStoreDocument(userIDAuth, dsDocumentWrite);
            DSDocument dsDocumentRead = cachedService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertArrayEquals(dsDocumentWrite.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue());
        }

        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_STORE_DOCUMENT));
        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_READ_DOCUMENT));
        cachedService.endTransaction(userIDAuth);
        LOGGER.debug(cachedService.toString());
        Assert.assertEquals(new Integer(1), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_STORE_DOCUMENT));
        Assert.assertEquals(new Integer(0), wrapper.counterMap.get(TransactionalDocumentSafeServiceTestWrapper.TX_READ_DOCUMENT));
    }



    @Test
    public void testTxDeleteFolder() {
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("petersPassword"));
        cachedService.createUser(userIDAuth);
        userIDAuthList.add(userIDAuth);

        DocumentFQN documentFQN = new DocumentFQN("folder1/file1.txt");

        {
            cachedService.beginTransaction(userIDAuth);
            Assert.assertFalse(cachedService.txDocumentExists(userIDAuth, documentFQN));
            TxBucketContentFQN bucketContentFQN = cachedService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
            Assert.assertTrue(bucketContentFQN.getFiles().isEmpty());
            DSDocument dsDocumentWrite = new DSDocument(
                    documentFQN,
                    new DocumentContent("content of file".getBytes())
            );
            cachedService.txStoreDocument(userIDAuth, dsDocumentWrite);
            DSDocument dsDocumentRead = cachedService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertArrayEquals(dsDocumentWrite.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue());
            cachedService.endTransaction(userIDAuth);
        }
        {
            cachedService.beginTransaction(userIDAuth);
            DSDocument dsDocumentWrite = new DSDocument(
                    documentFQN,
                    new DocumentContent("content of file 2".getBytes())
            );
            cachedService.txStoreDocument(userIDAuth, dsDocumentWrite);
            DSDocument dsDocumentRead = cachedService.txReadDocument(userIDAuth, documentFQN);
            Assert.assertArrayEquals(dsDocumentWrite.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue());
            TxBucketContentFQN bucketContentFQN = cachedService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
            Assert.assertEquals(1, bucketContentFQN.getFiles().size());
            cachedService.endTransaction(userIDAuth);
        }
        {
            cachedService.beginTransaction(userIDAuth);
            cachedService.txDeleteFolder(userIDAuth, documentFQN.getDocumentDirectory());
            cachedService.endTransaction(userIDAuth);
        }
        {
            cachedService.beginTransaction(userIDAuth);
            TxBucketContentFQN bucketContentFQN = cachedService.txListDocuments(userIDAuth, documentFQN.getDocumentDirectory(), ListRecursiveFlag.TRUE);
            Assert.assertEquals(0, bucketContentFQN.getFiles().size());
            cachedService.endTransaction(userIDAuth);
        }


    }
}

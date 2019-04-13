package org.adorsys.docusafe.transactional;

import com.googlecode.catchexception.CatchException;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.business.types.complex.*;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import de.adorsys.dfs.connection.api.types.ListRecursiveFlag;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@Slf4j
public class ParallelTxHistoryCleanupTest extends TransactionalDocumentSafeServiceBaseTest {
    private final static int MAX_COMMITED_TX_FOR_CLEANUP = 5;
    private SimpleRequestMemoryContextImpl secondRequestMemoryContext = new SimpleRequestMemoryContextImpl();
    private TransactionalDocumentSafeService secondTransactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(secondRequestMemoryContext, new DocumentSafeServiceImpl(DFSConnectionFactory.get()));

    @Test
    public void cleanUpWithParallelTransactionsTest() {
        StopWatch st = new StopWatch();
        st.start();

        transactionalDocumentSafeService.createUser(userIDAuth);

        for(int i = 0; i <= MAX_COMMITED_TX_FOR_CLEANUP; i++) {
            transactionalDocumentSafeService.beginTransaction(userIDAuth);
            transactionalDocumentSafeService.txStoreDocument(userIDAuth, createDocument("" + i));
            transactionalDocumentSafeService.endTransaction(userIDAuth);
        }

        // transaction with clean up
        transactionalDocumentSafeService.beginTransaction(userIDAuth);
        // update existing file 0
        DSDocument dsDocument = transactionalDocumentSafeService.txReadDocument(userIDAuth, new DocumentFQN("0"));
        DSDocument dsDocumentChanged = new DSDocument(
                dsDocument.getDocumentFQN(),
                new DocumentContent((new String(dsDocument.getDocumentContent().getValue()) + " updated").getBytes()));

        transactionalDocumentSafeService.txStoreDocument(userIDAuth, dsDocumentChanged);

        // delete existing file 1
        transactionalDocumentSafeService.txDeleteDocument(userIDAuth, new DocumentFQN("1"));

        // parallel Tx
        secondTransactionalDocumentSafeService.beginTransaction(userIDAuth);
        secondTransactionalDocumentSafeService.txStoreDocument(userIDAuth, createDocument("1"));

        // end of two parallel Tx
        transactionalDocumentSafeService.endTransaction(userIDAuth);
        CatchException.catchException(() -> secondTransactionalDocumentSafeService.endTransaction(userIDAuth));
        Assert.assertNotNull(CatchException.caughtException());

        List<DocumentFQN> list = dss.list(userIDAuth, new DocumentDirectoryFQN("/"), ListRecursiveFlag.TRUE);
        log.debug("LIST OF FILES IN DOCUMENTSAFE: " + list.toString());
        st.stop();
        log.debug("time for test " + st.toString());
    }
}

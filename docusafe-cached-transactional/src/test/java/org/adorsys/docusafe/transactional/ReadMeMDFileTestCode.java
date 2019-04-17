package org.adorsys.docusafe.transactional;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.dfs.connection.impl.factory.DFSConnectionFactory;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.business.impl.DocumentSafeServiceImpl;
import org.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.business.types.DSDocument;
import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import org.adorsys.docusafe.cached.transactional.impl.CachedTransactionalDocumentSafeServiceImpl;
import org.adorsys.docusafe.service.api.types.UserIDAuth;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by peter on 19.02.19 09:45.
 */
public class ReadMeMDFileTestCode {
    public static void main(String[] args) {
         // create service
        CachedTransactionalDocumentSafeService cachedTransactionalDocumentSafeService;
        {
            org.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl simpleRequestMemoryContext = new org.adorsys.docusafe.cached.transactional.impl.SimpleRequestMemoryContextImpl();
            DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(DFSConnectionFactory.get());
            TransactionalDocumentSafeService transactionalDocumentSafeService = new TransactionalDocumentSafeServiceImpl(simpleRequestMemoryContext, documentSafeService);
            cachedTransactionalDocumentSafeService = new CachedTransactionalDocumentSafeServiceImpl(simpleRequestMemoryContext, transactionalDocumentSafeService, documentSafeService);
        }

        // create user
        UserIDAuth userIDAuth = new UserIDAuth(new UserID("peter"), new ReadKeyPassword("passwordOfPeter"));
        cachedTransactionalDocumentSafeService.createUser(userIDAuth);

        // begin Transaction
        cachedTransactionalDocumentSafeService.beginTransaction(userIDAuth);

        // create document
        DocumentFQN documentFQN = new DocumentFQN("first/document.txt");
        DocumentContent documentContent = new DocumentContent(("programming is the mirror of your mind").getBytes());
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent);
        cachedTransactionalDocumentSafeService.txStoreDocument(userIDAuth, dsDocument);

        // read the document again
        DSDocument dsDocumentRead = cachedTransactionalDocumentSafeService.txReadDocument(userIDAuth, documentFQN);
        if (Arrays.equals(dsDocument.getDocumentContent().getValue(), dsDocumentRead.getDocumentContent().getValue()) == true) {
            System.out.println("read the following content from " + documentFQN + ":" + new String(dsDocumentRead.getDocumentContent().getValue()));
        } else {
            throw new BaseException("This will never happen :-)");
        }

        // end Transaction
        cachedTransactionalDocumentSafeService.endTransaction(userIDAuth);
    }

    public static class SimpleRequestMemoryContextImpl extends HashMap<Object, Object> {
    }
}

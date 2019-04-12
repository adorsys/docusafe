package org.adorsys.docusafe.transactional;

import org.adorsys.cryptoutils.exceptions.BaseException;
import org.adorsys.cryptoutils.storeconnectionfactory.ExtendedStoreConnectionFactory;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.business.types.complex.DSDocument;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.cached.transactional.CachedTransactionalDocumentSafeService;
import org.adorsys.docusafe.cached.transactional.impl.CachedTransactionalDocumentSafeServiceImpl;
import org.adorsys.docusafe.service.types.DocumentContent;
import org.adorsys.docusafe.transactional.impl.TransactionalDocumentSafeServiceImpl;
import org.adorsys.encobject.domain.ReadKeyPassword;

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
            DocumentSafeService documentSafeService = new DocumentSafeServiceImpl(ExtendedStoreConnectionFactory.get());
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
        DSDocument dsDocument = new DSDocument(documentFQN, documentContent, null);
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
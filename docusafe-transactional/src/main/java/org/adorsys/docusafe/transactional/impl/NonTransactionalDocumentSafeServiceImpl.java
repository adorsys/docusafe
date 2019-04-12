package org.adorsys.docusafe.transactional.impl;

import org.adorsys.cryptoutils.exceptions.BaseException;
import org.adorsys.docusafe.business.DocumentSafeService;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.transactional.NonTransactionalDocumentSafeService;
import org.adorsys.encobject.types.ListRecursiveFlag;
import org.adorsys.encobject.types.OverwriteFlag;
import org.adorsys.encobject.types.PublicKeyJWK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by peter on 15.08.18 at 11:57.
 */
public class NonTransactionalDocumentSafeServiceImpl implements NonTransactionalDocumentSafeService {
    private final static Logger LOGGER = LoggerFactory.getLogger(NonTransactionalDocumentSafeServiceImpl.class);
    protected DocumentSafeService documentSafeService;

    public NonTransactionalDocumentSafeServiceImpl(DocumentSafeService documentSafeService) {
        LOGGER.debug("new Instance of TransactionalDocumentSafeServiceImpl");
        this.documentSafeService = documentSafeService;
    }
    // ============================================================================================
    // NON-TRANSACTIONAL FOR OWNER
    // ============================================================================================
    @Override
    public void createUser(UserIDAuth userIDAuth) {
        documentSafeService.createUser(userIDAuth);
    }

    @Override
    public void destroyUser(UserIDAuth userIDAuth) {
        documentSafeService.destroyUser(userIDAuth);
    }

    @Override
    public boolean userExists(UserID userID) {
        return documentSafeService.userExists(userID);
    }

    @Override
    public PublicKeyJWK findPublicEncryptionKey(UserID userID) {
        return documentSafeService.findPublicEncryptionKey(userID);
    }

    // ============================================================================================
    // INBOX STUFF
    // ============================================================================================
    @Override
    public BucketContentFQNWithUserMetaData nonTxListInbox(UserIDAuth userIDAuth) {
        return documentSafeService.listInbox(userIDAuth);
    }

}

package de.adorsys.docusafe.transactional.impl;

import de.adorsys.docusafe.business.DocumentSafeService;
import de.adorsys.docusafe.business.types.DFSCredentials;
import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import de.adorsys.docusafe.service.api.types.UserID;
import de.adorsys.docusafe.service.api.types.UserIDAuth;
import de.adorsys.docusafe.transactional.NonTransactionalDocumentSafeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    public void changeUserPassword(UserIDAuth userIDAuth, ReadKeyPassword newPassword) {
        documentSafeService.changeUserPassword(userIDAuth, newPassword);
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
    public void registerDFSCredentials(UserIDAuth userIDAuth, DFSCredentials dfsCredentials) {
        documentSafeService.registerDFSCredentials(userIDAuth, dfsCredentials);
    }

    // ============================================================================================
    // INBOX STUFF
    // ============================================================================================
    @Override
    public List<DocumentFQN> nonTxListInbox(UserIDAuth userIDAuth) {
        return documentSafeService.listInbox(userIDAuth);
    }

}

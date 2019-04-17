package org.adorsys.docusafe.transactional;

import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.service.api.types.UserID;
import org.adorsys.docusafe.service.api.types.UserIDAuth;

import java.util.List;

/**
 * Created by peter on 15.08.18 at 11:55.
 */
public interface NonTransactionalDocumentSafeService {

    // NON-TRANSACTIONAL FOR OWNER
    void createUser(UserIDAuth userIDAuth);
    void destroyUser(UserIDAuth userIDAuth);
    boolean userExists(UserID userID);

    // INBOX STUFF
    /**
     * This methods rereads the inbox every time it is called. so even in one tx
     * the context can change.
     *
     * @param userIDAuth user and password
     * @return the recursive list of files found in the inbox
     */
    List<DocumentFQN> nonTxListInbox(UserIDAuth userIDAuth);

}

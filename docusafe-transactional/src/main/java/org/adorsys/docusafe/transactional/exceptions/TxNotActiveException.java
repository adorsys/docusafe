package org.adorsys.docusafe.transactional.exceptions;

import org.adorsys.docusafe.service.api.types.UserID;

/**
 * Created by peter on 29.09.18 at 11:43.
 */
public class TxNotActiveException extends TxBaseException {
    public TxNotActiveException(UserID userID) {
        super("no transaction active for " + userID);
    }
}

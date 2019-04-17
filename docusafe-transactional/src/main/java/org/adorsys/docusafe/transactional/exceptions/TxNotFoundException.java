package org.adorsys.docusafe.transactional.exceptions;

import org.adorsys.docusafe.business.types.DocumentFQN;
import org.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import org.adorsys.docusafe.transactional.types.TxID;

/**
 * Created by peter on 13.06.18 at 18:17.
 */
public class TxNotFoundException extends TxBaseException {
    public TxNotFoundException(DocumentFQN file, LastCommitedTxID lastTxID) {
        super(file.getValue() + " not found for last known transaction " + lastTxID.getValue());
    }
    public TxNotFoundException(TxID txid) {
        super("no tx found with id " + txid.getValue());
    }
}

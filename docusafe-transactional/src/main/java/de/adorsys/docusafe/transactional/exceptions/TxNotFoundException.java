package de.adorsys.docusafe.transactional.exceptions;

import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import de.adorsys.docusafe.transactional.types.TxID;

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

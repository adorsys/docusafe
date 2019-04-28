package de.adorsys.docusafe.transactional.impl;

import de.adorsys.docusafe.transactional.types.TxID;

/**
 * Created by peter on 11.06.18 at 16:07.
 */
public class LastCommitedTxID extends TxID {
    public LastCommitedTxID(String lastTxID) {
        super(lastTxID);
    }
}

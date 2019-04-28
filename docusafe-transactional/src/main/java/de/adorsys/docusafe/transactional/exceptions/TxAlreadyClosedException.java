package de.adorsys.docusafe.transactional.exceptions;

import de.adorsys.docusafe.transactional.types.TxID;

/**
 * Created by peter on 13.06.18 at 18:01.
 */
public class TxAlreadyClosedException extends TxBaseException {
    public TxAlreadyClosedException(TxID txid) {
        super(txid.getValue());
    }
}

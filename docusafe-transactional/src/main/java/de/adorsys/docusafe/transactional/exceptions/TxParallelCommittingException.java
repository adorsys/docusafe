package de.adorsys.docusafe.transactional.exceptions;

import de.adorsys.docusafe.transactional.types.TxID;

public class TxParallelCommittingException extends TxRacingConditionException {
    public TxParallelCommittingException(TxID currentTx, TxID lastTx, String conflictFile) {
        super(currentTx, lastTx, conflictFile);
    }
}

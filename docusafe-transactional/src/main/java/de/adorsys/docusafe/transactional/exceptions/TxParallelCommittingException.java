package de.adorsys.docusafe.transactional.exceptions;

import de.adorsys.docusafe.business.types.DocumentFQN;
import de.adorsys.docusafe.transactional.types.TxID;

public class TxParallelCommittingException extends TxRacingConditionException {
    private DocumentFQN conflictDocument;

    public TxParallelCommittingException(TxID currentTx, TxID lastTx, DocumentFQN conflictFile) {
        super(currentTx, lastTx, conflictFile);
        conflictDocument = conflictFile;
    }

    public DocumentFQN getConflictDocument() {
        return conflictDocument;
    }
}

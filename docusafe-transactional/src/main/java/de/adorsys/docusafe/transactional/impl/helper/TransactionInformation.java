package de.adorsys.docusafe.transactional.impl.helper;

import de.adorsys.docusafe.transactional.types.TxID;

import java.util.Date;

public class TransactionInformation {
    private Date txDateFrom;
    private Date txDateUntil;
    private TxID previousTxID;
    private TxID currentTxID;

    public TransactionInformation(Date txDateFrom, Date txDateUntil, TxID previousTxID, TxID currentTxID) {
        this.txDateFrom = txDateFrom;
        this.txDateUntil = txDateUntil;
        this.previousTxID = previousTxID;
        this.currentTxID = currentTxID;
    }

    public Date getTxDateFrom() {
        return txDateFrom;
    }

    public Date getTxDateUntil() {
        return txDateUntil;
    }

    public TxID getPreviousTxID() {
        return previousTxID;
    }

    public TxID getCurrentTxID() {
        return currentTxID;
    }
}

package de.adorsys.docusafe.transactional.exceptions;

/**
 * Created by peter on 29.09.18 at 16:13.
 */
public class TxInnerException extends TxBaseException {
    public TxInnerException() {
        super("INNER TRANSACTION NOT ALLOWED YET");
    }
}

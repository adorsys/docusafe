package de.adorsys.docusafe.transactional.exceptions;

import de.adorsys.docusafe.business.types.DocumentFQN;

/**
 * Created by peter on 13.06.18 at 19:35.
 */
public class TxGrantedDocumentMustNotContainFolderException extends TxBaseException {
    public TxGrantedDocumentMustNotContainFolderException(DocumentFQN documentFQN) {
        super(documentFQN.getValue());
    }
}

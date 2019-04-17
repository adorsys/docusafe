package org.adorsys.docusafe.transactional.types;

import org.adorsys.docusafe.business.types.DocumentFQN;

/**
 * Created by peter on 30.01.19 10:02.
 */
public class TxDocumentFQNWithVersion {
    private final DocumentFQN documentFQN;
    private final TxDocumentFQNVersion version;

    public TxDocumentFQNWithVersion(DocumentFQN documentFQN, TxDocumentFQNVersion version) {
        this.documentFQN = documentFQN;
        this.version = version;
    }

    public DocumentFQN getDocumentFQN() {
        return documentFQN;
    }

    public TxDocumentFQNVersion getVersion() {
        return version;
    }
}

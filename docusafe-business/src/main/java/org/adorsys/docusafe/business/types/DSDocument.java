package org.adorsys.docusafe.business.types;

import org.adorsys.docusafe.service.api.types.DocumentContent;

/**
 * Created by peter on 22.01.18 at 08:14.
 * DocumentSafeDocument -> DSDocument
 */
public class DSDocument {
    private DocumentFQN documentFQN;
    private DocumentContent documentContent;

    public DSDocument(DocumentFQN documentFQN, DocumentContent documentContent) {
        this.documentFQN = documentFQN;
        this.documentContent = documentContent;
    }

    public DocumentFQN getDocumentFQN() {
        return documentFQN;
    }

    public DocumentContent getDocumentContent() {
        return documentContent;
    }
}

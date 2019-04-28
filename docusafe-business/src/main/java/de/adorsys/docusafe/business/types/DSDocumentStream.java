package de.adorsys.docusafe.business.types;

import java.io.InputStream;

/**
 * Created by peter on 05.03.18 at 08:18.
 */
public class DSDocumentStream {
    private DocumentFQN documentFQN;
    private InputStream documentStream;

    public DSDocumentStream(DocumentFQN documentFQN, InputStream documentStream) {
        this.documentFQN = documentFQN;
        this.documentStream = documentStream;
    }

    public DocumentFQN getDocumentFQN() {
        return documentFQN;
    }

    public InputStream getDocumentStream() {
        return documentStream;
    }

}

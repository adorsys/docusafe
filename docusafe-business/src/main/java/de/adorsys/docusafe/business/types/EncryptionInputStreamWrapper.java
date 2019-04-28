package de.adorsys.docusafe.business.types;

import java.io.IOException;
import java.io.InputStream;

public class EncryptionInputStreamWrapper extends InputStream {
    private InputStream origStream;
    private InputStream innerStream;

    public EncryptionInputStreamWrapper(InputStream origStream, InputStream innerStream) {
        this.origStream = origStream;
        this.innerStream = innerStream;
    }

    @Override
    public int read() throws IOException {
        return origStream.read();
    }

    @Override
    public void close() throws IOException {
        innerStream.close();
        origStream.close();
    }
}

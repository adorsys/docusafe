package org.adorsys.docusafe.transactional.impl.helper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.adorsys.docusafe.business.types.DocumentFQN;

import java.io.IOException;

/**
 * Created by peter on 12.06.18 at 14:24.
 */
public class DocumentFQNJsonAdapter  extends TypeAdapter<DocumentFQN> {
    @Override
    public void write(JsonWriter out, DocumentFQN documentFQN) throws IOException {
        out.value(documentFQN.getValue());
    }
    @Override
    public DocumentFQN read(JsonReader in) throws IOException {
        return new DocumentFQN(in.nextString());
    }
}

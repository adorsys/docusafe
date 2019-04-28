package de.adorsys.docusafe.transactional.impl.helper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.adorsys.docusafe.transactional.types.TxID;

import java.io.IOException;

/**
 * Created by peter on 12.06.18 at 13:29.
 */
public class TxIDJsonAdapter  extends TypeAdapter<TxID> {
    @Override
    public void write(JsonWriter out, TxID txid) throws IOException {
        out.value(txid.getValue());
    }
    @Override
    public TxID read(JsonReader in) throws IOException {
        return new TxID(in.nextString());
    }
}

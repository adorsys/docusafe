package de.adorsys.docusafe.transactional.impl.helper;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import de.adorsys.docusafe.transactional.types.TxID;

import java.io.IOException;

/**
 * Created by peter on 12.06.18 at 13:46.
 */
public class LastCommitedTxIDJsonAdapter extends TypeAdapter<LastCommitedTxID> {

    @Override
    public void write(JsonWriter out, LastCommitedTxID txid) throws IOException {
        if (txid != null) {
            out.value(txid.getValue());
        } else {
            out.value("null");
        }
    }

    @Override
    public LastCommitedTxID read(JsonReader in) throws IOException {
        String s = in.nextString();
        if (s.equalsIgnoreCase("null")) {
            return null;
        }
        return new LastCommitedTxID(s);
    }
}

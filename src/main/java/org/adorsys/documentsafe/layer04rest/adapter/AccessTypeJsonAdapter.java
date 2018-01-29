package org.adorsys.documentsafe.layer04rest.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.adorsys.documentsafe.layer01persistence.types.BucketName;
import org.adorsys.documentsafe.layer03business.types.AccessType;

import java.io.IOException;

/**
 * Created by peter on 13.01.18.
 */
public class AccessTypeJsonAdapter extends TypeAdapter<AccessType> {
    @Override
    public void write(JsonWriter out, AccessType accessType) throws IOException {
        out.value(accessType.name());
    }
    @Override
    public AccessType read(JsonReader in) throws IOException {
        return AccessType.valueOf(in.nextString());
    }
}
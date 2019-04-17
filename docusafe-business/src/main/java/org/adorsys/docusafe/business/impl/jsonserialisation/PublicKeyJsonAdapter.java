package org.adorsys.docusafe.business.impl.jsonserialisation;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.common.utils.HexUtil;

import java.io.*;

import java.io.IOException;
import java.security.PublicKey;

public class PublicKeyJsonAdapter extends TypeAdapter<PublicKey> {
    @Override
    public void write(JsonWriter out, PublicKey publicKey) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(publicKey);

        out.value(HexUtil.convertBytesToHexString(bos.toByteArray()));
    }
    @Override
    public PublicKey read(JsonReader in) throws IOException {
        try {
            byte[] bytes = HexUtil.convertHexStringToBytes(in.nextString());
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream is = new ObjectInputStream(bis);
            return (PublicKey) is.readObject();
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }
}

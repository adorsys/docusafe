package de.adorsys.docusafe.business.impl.jsonserialisation;

import com.google.gson.*;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.dfs.connection.api.domain.Payload;
import de.adorsys.dfs.connection.api.service.impl.SimplePayloadImpl;
import de.adorsys.docusafe.business.types.DFSCredentials;
import de.adorsys.docusafe.service.api.keystore.types.PublicKeyList;
import de.adorsys.docusafe.service.api.keystore.types.PublicKeyIDWithPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.security.PublicKey;
import java.util.List;

/**
 * Created by peter on 11.06.18 at 17:04.
 */
public class Class2JsonHelper {
    private final static Logger LOGGER = LoggerFactory.getLogger(Class2JsonHelper.class);
    private final static String CHARSET = "UTF-8";
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";

    private Gson gson = new GsonBuilder().setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(PublicKey.class, new PublicKeyJsonAdapter())
            .setDateFormat(DATE_FORMAT_STRING)
            .create();

    public Payload dfsCredentialsToContent(final DFSCredentials dfsConnection) {
        try {
            String s = gson.toJson(dfsConnection);
            return new SimplePayloadImpl(s.getBytes(CHARSET));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public DFSCredentials contentToDFSConnection(Payload payload) {
        try {
            String jsonString = new String(payload.getData(), CHARSET);
            return gson.fromJson(jsonString, DFSCredentials.class);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public Payload keyListToContent(final List<PublicKeyIDWithPublicKey> list) {
        try {
            PublicKeyList publicKeyList = new PublicKeyList();
            publicKeyList.addAll(list);
            String s = gson.toJson(publicKeyList);
            return new SimplePayloadImpl(s.getBytes(CHARSET));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public List<PublicKeyIDWithPublicKey> contentToKeyList(Payload payload) {
        try {
            String jsonString = new String(payload.getData(), CHARSET);
            return  gson.fromJson(jsonString, PublicKeyList.class);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    /**
     * Created by peter on 21.02.18 at 08:58.
     * Author: Mauricio Silva Manrique
     * http://technology.finra.org/code/serialize-deserialize-interfaces-in-java.html
     */
    private static class InterfaceAdapter<T> implements JsonSerializer<T>, JsonDeserializer<T> {

        private static final String CLASSNAME = "CLASSNAME";
        private static final String DATA = "DATA";

        @Override
        public T deserialize(JsonElement jsonElement, Type type,
                             JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            JsonPrimitive prim = (JsonPrimitive) jsonObject.get(CLASSNAME);
            String className = prim.getAsString();
            Class klass = getObjectClass(className);
            return jsonDeserializationContext.deserialize(jsonObject.get(DATA), klass);
        }

        @Override
        public JsonElement serialize(T jsonElement, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(CLASSNAME, jsonElement.getClass().getName());
            jsonObject.add(DATA, jsonSerializationContext.serialize(jsonElement));
            return jsonObject;
        }

        /****** Helper method to get the className of the object to be deserialized *****/
        public Class getObjectClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                //e.printStackTrace();
                throw new JsonParseException(e.getMessage());
            }
        }
    }
}

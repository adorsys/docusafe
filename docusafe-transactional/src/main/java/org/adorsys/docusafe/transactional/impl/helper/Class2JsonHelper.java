package org.adorsys.docusafe.transactional.impl.helper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.adorsys.common.exceptions.BaseExceptionHandler;
import org.adorsys.docusafe.business.types.complex.DocumentFQN;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.adorsys.docusafe.transactional.impl.LastCommitedTxID;
import org.adorsys.docusafe.transactional.impl.TxIDHashMapWrapper;
import org.adorsys.docusafe.transactional.impl.TxIDLog;
import org.adorsys.docusafe.transactional.types.TxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * Created by peter on 11.06.18 at 17:04.
 */
public class Class2JsonHelper {
    private final static Logger LOGGER = LoggerFactory.getLogger(Class2JsonHelper.class);
    private final static String CHARSET = "UTF-8";
    public static final String DATE_FORMAT_STRING = "yyyy-MM-dd HH:mm:ss.SSS";

    private Gson gson = new GsonBuilder().setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(TxID.class, new TxIDJsonAdapter())
            .registerTypeAdapter(LastCommitedTxID.class, new LastCommitedTxIDJsonAdapter())
            .registerTypeAdapter(DocumentFQN.class, new DocumentFQNJsonAdapter())
            .setDateFormat(DATE_FORMAT_STRING)
            .create();

    public TxIDHashMapWrapper txidHashMapFromContent(DocumentContent documentContent) {
        try {
            String jsonString = new String(documentContent.getValue(), CHARSET);
            LOGGER.trace("content to hashmap:" + jsonString);
            return gson.fromJson(jsonString, TxIDHashMapWrapper.class);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public DocumentContent txidHashMapToContent(TxIDHashMapWrapper txIDHashMapWrapper) {
        try {
            String s = gson.toJson(txIDHashMapWrapper);
            LOGGER.trace("hashmap to content:" + s);
            return new DocumentContent(s.getBytes(CHARSET));
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public TxIDLog txidLogFromContent(DocumentContent documentContent) {
        try {
            String jsonString = new String(documentContent.getValue(), CHARSET);
            LOGGER.trace("content to txidlog:" + jsonString);
            return gson.fromJson(jsonString, TxIDLog.class);
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }
    }

    public DocumentContent txidLogToContent(TxIDLog txidLog) {
        try {
            String s = gson.toJson(txidLog);
            LOGGER.trace("txidlog to content:" + s);
            return new DocumentContent(s.getBytes(CHARSET));
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

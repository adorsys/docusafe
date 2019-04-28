package de.adorsys.docusafe.transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 09.07.18 at 14:06.
 */
public class SimpleRequestMemoryContextImpl implements RequestMemoryContext {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleRequestMemoryContextImpl.class);
    private Map<String, TransactionalContext> pseudoUserMap = new HashMap<>();
    TransactionalContext current = null;

    @Override
    public void put(Object key, Object value) {
        current.put(key, value);
    }

    @Override
    public Object get(Object key) {
        return current.get(key);
    }

    public SimpleRequestMemoryContextImpl() {
        switchToUser(1);
    }

    public void switchToUser(int i) {
        String key = "" + i;
        if (!pseudoUserMap.containsKey(key)) {
            LOGGER.debug("User is new :" + key);
            pseudoUserMap.put(key, new TransactionalContext(key));
        } else {
            LOGGER.debug("User is known :" + key);
        }
        current = pseudoUserMap.get(key);
        LOGGER.debug("current tx context " + current);
    }

    public static class TransactionalContext extends HashMap<Object, Object> {
        private static String KEY = "KEY";
        public TransactionalContext(String key) {
            put(KEY, key);
        }

        @Override
        public String toString() {
            return "TransactionalContext{"+get(KEY)+"}";
        }
    }

}

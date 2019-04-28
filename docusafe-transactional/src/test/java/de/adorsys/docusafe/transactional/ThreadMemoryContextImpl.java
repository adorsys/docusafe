package de.adorsys.docusafe.transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 03.12.18 13:31.
 */
public class ThreadMemoryContextImpl implements RequestMemoryContext {
    private final static Logger LOGGER = LoggerFactory.getLogger(ThreadMemoryContextImpl.class);
    private Map<String, ThreadMemoryContextImpl.TransactionalContext> pseudoUserMap = new HashMap<>();

    @Override
    public void put(Object key, Object value) {
        getCurrent().put(key, value);
    }

    @Override
    public Object get(Object key) {
        return getCurrent().get(key);
    }

    private ThreadMemoryContextImpl.TransactionalContext getCurrent() {
        String key = Thread.currentThread().getName();
        if (!pseudoUserMap.containsKey(key)) {
            LOGGER.debug("Thread is new :" + key);
            pseudoUserMap.put(key, new ThreadMemoryContextImpl.TransactionalContext());
        } else {
            LOGGER.debug("Thead is known :" + key);
        }
        return pseudoUserMap.get(key);
    }

    public static class TransactionalContext extends HashMap<Object, Object> {
    }
}

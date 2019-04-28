package de.adorsys.docusafe.cached.transactional.impl;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.docusafe.transactional.RequestMemoryContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 09.07.18 at 11:41.
 */
public class SimpleRequestMemoryContextImpl implements RequestMemoryContext {
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
            pseudoUserMap.put(key, new TransactionalContext());
        }
        current = pseudoUserMap.get(key);
    }

    public static void selfTest() {
        SimpleRequestMemoryContextImpl requestMemoryContext = new SimpleRequestMemoryContextImpl();
        requestMemoryContext.put("1","v1");
        String s = (String) requestMemoryContext.get("1");
        if (!"v1".equals(s)) {
            throw new BaseException("ERROR");
        }
        requestMemoryContext.put("2","v2");
        s = (String) requestMemoryContext.get("2");
        if (!"v2".equals(s)) {
            throw new BaseException("ERROR");
        }
        requestMemoryContext.switchToUser(2);
        if (requestMemoryContext.get("2") != null) {
            throw new BaseException("ERROR");
        }
        requestMemoryContext.put("2","otherUserv2");
        requestMemoryContext.switchToUser(1);
        s = (String) requestMemoryContext.get("2");
        if (!"v2".equals(s)) {
            throw new BaseException("ERROR");
        }
        requestMemoryContext.switchToUser(2);
        s = (String) requestMemoryContext.get("2");
        if (!"otherUserv2".equals(s)) {
            throw new BaseException("ERROR");
        }
    }

    public static class TransactionalContext extends HashMap<Object, Object> {
    }

}

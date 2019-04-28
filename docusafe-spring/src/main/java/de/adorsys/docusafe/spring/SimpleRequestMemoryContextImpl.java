package de.adorsys.docusafe.spring;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.docusafe.transactional.RequestMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Created by peter on 09.07.18 at 14:06.
 */
public class SimpleRequestMemoryContextImpl implements RequestMemoryContext {
    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleRequestMemoryContextImpl.class);
    private final static int SCOPE = 1;

    @Override
    public void put(Object key, Object value) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new BaseException("requestAttributes are null");
        }
        if (key instanceof  String) {
            String aKey = (String) key;
            requestAttributes.setAttribute(aKey, value, SCOPE);
            return;
        }
        throw new BaseException("key is not of Stringtype but " + key.getClass().getName());
    }

    @Override
    public Object get(Object key) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new BaseException("requestAttributes are null for " + key);
        }
        if (key instanceof  String) {
            String aKey = (String) key;
            return requestAttributes.getAttribute(aKey, SCOPE);
        }
        throw new BaseException("key is not of Stringtype but " + key.getClass().getName());
    }
}

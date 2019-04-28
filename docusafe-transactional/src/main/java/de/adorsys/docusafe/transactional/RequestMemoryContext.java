package de.adorsys.docusafe.transactional;

/**
 * Created by peter on 09.07.18 at 11:37.
 */
/* Scope of an instance (not provided by docusafe) must be a real request.

  */
public interface RequestMemoryContext {
    void put(Object key, Object value);
    Object get(Object key);
}

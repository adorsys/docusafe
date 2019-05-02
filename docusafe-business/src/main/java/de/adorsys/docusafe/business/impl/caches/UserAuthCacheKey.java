package de.adorsys.docusafe.business.impl.caches;

import de.adorsys.docusafe.service.api.types.UserIDAuth;

public class UserAuthCacheKey implements Comparable {
    private final String key;

    public UserAuthCacheKey(UserIDAuth userIDAuth) {
        key = userIDAuth.getUserID().getValue() + "-" + userIDAuth.getReadKeyPassword().getValue().hashCode();
    }

    @Override
    public int compareTo(Object o) {
        if (! (o instanceof UserAuthCacheKey)) {
            return -1;
        }
        return key.compareTo(((UserAuthCacheKey) o).key);
    }
}

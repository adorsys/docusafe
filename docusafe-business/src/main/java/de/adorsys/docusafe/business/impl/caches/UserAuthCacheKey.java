package de.adorsys.docusafe.business.impl.caches;

import de.adorsys.docusafe.service.api.types.UserIDAuth;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAuthCacheKey that = (UserAuthCacheKey) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}

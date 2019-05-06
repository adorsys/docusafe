package de.adorsys.docusafe.business.impl.dummyCaches;

import de.adorsys.docusafe.business.impl.caches.guava.UserPublicKeyListCacheGuavaImpl;
import de.adorsys.docusafe.service.api.keystore.types.PublicKeyList;
import de.adorsys.docusafe.service.api.types.UserID;

public class UserPublicKeyListCacheNoCacheTestImpl extends UserPublicKeyListCacheGuavaImpl {
    @Override
    public PublicKeyList get(UserID key) {
        return null;
    }

    @Override
    public void put(UserID key, PublicKeyList value) {
    }

    @Override
    public void remove(UserID key) {
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " size:" + this.size();
    }


}

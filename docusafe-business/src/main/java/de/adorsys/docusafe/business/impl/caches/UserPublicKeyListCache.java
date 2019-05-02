package de.adorsys.docusafe.business.impl.caches;

import de.adorsys.docusafe.service.api.keystore.types.PublicKeyList;

public interface UserPublicKeyListCache  extends DocusafeCacheTemplate<UserAuthCacheKey, PublicKeyList> {
}

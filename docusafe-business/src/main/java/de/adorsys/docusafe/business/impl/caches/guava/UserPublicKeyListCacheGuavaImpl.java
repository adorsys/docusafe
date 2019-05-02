package de.adorsys.docusafe.business.impl.caches.guava;

import de.adorsys.docusafe.business.impl.caches.UserAuthCacheKey;
import de.adorsys.docusafe.business.impl.caches.UserPublicKeyListCache;
import de.adorsys.docusafe.service.api.keystore.types.PublicKeyList;

public class UserPublicKeyListCacheGuavaImpl extends DocusafeCacheTemplateGuavaImpl<UserAuthCacheKey, PublicKeyList> implements UserPublicKeyListCache {
}

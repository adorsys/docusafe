package de.adorsys.docusafe.business.impl.caches.guava;

import de.adorsys.docusafe.business.impl.caches.UserPublicKeyListCache;
import de.adorsys.docusafe.service.api.keystore.types.PublicKeyList;
import de.adorsys.docusafe.service.api.types.UserID;

public class UserPublicKeyListCacheGuavaImpl extends DocusafeCacheTemplateGuavaImpl<UserID, PublicKeyList> implements UserPublicKeyListCache {
}

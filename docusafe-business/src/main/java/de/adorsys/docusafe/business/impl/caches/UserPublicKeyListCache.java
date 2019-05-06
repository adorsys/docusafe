package de.adorsys.docusafe.business.impl.caches;

import de.adorsys.docusafe.service.api.keystore.types.PublicKeyList;
import de.adorsys.docusafe.service.api.types.UserID;

public interface UserPublicKeyListCache  extends DocusafeCacheTemplate<UserID, PublicKeyList> {
}

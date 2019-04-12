package org.adorsys.docusafe.business.impl.caches;

import org.adorsys.docusafe.service.api.keystore.types.ReadKeyPassword;
import org.adorsys.docusafe.service.api.types.UserID;

/**
 * Created by peter on 26.06.18 at 18:07.
 */
public interface UserAuthCache extends DocusafeCacheTemplate<UserID, ReadKeyPassword> {
}

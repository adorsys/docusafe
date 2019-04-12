package org.adorsys.docusafe.service.api.keystore;

import org.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;
import org.adorsys.docusafe.service.api.types.UserIDAuth;

/**
 * Acts as a private keys database.
 */
public interface PrivateKeyService {

    KeyStoreAccess keystore(UserIDAuth forUser);

}

package org.adorsys.docusafe.service.api.keystore;


import org.adorsys.docusafe.service.api.types.PublicKeyWithId;
import org.adorsys.docusafe.service.api.types.UserID;

/**
 * Acts as a public keys database.
 */
public interface PublicKeyService {

    PublicKeyWithId publicKey(UserID forUser);
}

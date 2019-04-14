package org.adorsys.docusafe.service.api.cmsencryption;

import de.adorsys.dfs.connection.api.domain.Payload;
import org.adorsys.docusafe.service.api.keystore.types.KeyID;
import org.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.bouncycastle.cms.CMSEnvelopedData;

import java.security.PublicKey;

public interface CMSEncryptionService {

    CMSEnvelopedData encrypt(Payload paylaod, PublicKey publicKey, KeyID publicKeyID);

    Payload decrypt(CMSEnvelopedData cmsEnvelopedData, KeyStoreAccess keyStoreAccess);
}

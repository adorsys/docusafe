package org.adorsys.docusafe.service.api.cmsencryption;

import org.adorsys.docusafe.service.api.keystore.types.KeyID;
import org.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.bouncycastle.cms.CMSEnvelopedData;

import java.security.PublicKey;

public interface CMSEncryptionService {

    CMSEnvelopedData encrypt(DocumentContent data, PublicKey publicKey, KeyID publicKeyID);

    DocumentContent decrypt(CMSEnvelopedData cmsEnvelopedData, KeyStoreAccess keyStoreAccess);
}

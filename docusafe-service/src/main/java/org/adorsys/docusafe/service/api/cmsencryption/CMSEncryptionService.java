package org.adorsys.docusafe.service.api.cmsencryption;

import de.adorsys.dfs.connection.api.domain.Payload;
import org.adorsys.docusafe.service.api.keystore.types.KeyID;
import org.adorsys.docusafe.service.api.keystore.types.KeyStoreAccess;
import org.adorsys.docusafe.service.api.types.DocumentContent;
import org.bouncycastle.cms.CMSEnvelopedData;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;

public interface CMSEncryptionService {

    CMSEnvelopedData encrypt(Payload paylaod, PublicKey publicKey, KeyID publicKeyID);

    Payload decrypt(CMSEnvelopedData cmsEnvelopedData, KeyStoreAccess keyStoreAccess);

    InputStream buildEncryptionInputStream(InputStream dataContentStream, PublicKey publicKey, KeyID publicKeyID);

    InputStream buildEncryptionInputStream(InputStream dataContentStream, SecretKey secretKey, KeyID secretKeyID);

    InputStream buildDecryptionInputStream(InputStream inputStream, KeyStoreAccess keyStoreAccess);
}

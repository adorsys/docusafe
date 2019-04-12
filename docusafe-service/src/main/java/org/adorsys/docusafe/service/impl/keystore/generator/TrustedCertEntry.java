package org.adorsys.docusafe.service.impl.keystore.generator;


import org.adorsys.docusafe.service.api.keystore.types.KeyEntry;
import org.bouncycastle.cert.X509CertificateHolder;

public interface TrustedCertEntry extends KeyEntry {
    X509CertificateHolder getCertificate();
}

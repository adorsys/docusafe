package de.adorsys.docusafe.service.impl.keystore.generator;


import de.adorsys.docusafe.service.api.keystore.types.KeyEntry;
import org.bouncycastle.cert.X509CertificateHolder;

public interface TrustedCertEntry extends KeyEntry {
    X509CertificateHolder getCertificate();
}

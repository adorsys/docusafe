package de.adorsys.docusafe.service.api.keystore.types;


public interface KeyPairEntry extends KeyEntry {
    SelfSignedKeyPairData getKeyPair();

    CertificationResult getCertification();
}

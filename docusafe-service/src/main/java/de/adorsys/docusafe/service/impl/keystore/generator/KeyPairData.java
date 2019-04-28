package de.adorsys.docusafe.service.impl.keystore.generator;

import de.adorsys.docusafe.service.api.keystore.types.CertificationResult;
import de.adorsys.docusafe.service.api.keystore.types.KeyPairEntry;
import de.adorsys.docusafe.service.api.keystore.types.SelfSignedKeyPairData;
import lombok.Builder;
import lombok.Getter;

import javax.security.auth.callback.CallbackHandler;

@Getter
public class KeyPairData extends KeyEntryData implements KeyPairEntry {

    private final SelfSignedKeyPairData keyPair;

    private final CertificationResult certification;

    @Builder
    private KeyPairData(CallbackHandler passwordSource, String alias, SelfSignedKeyPairData keyPair, CertificationResult certification) {
        super(passwordSource, alias);
        this.keyPair = keyPair;
        this.certification = certification;
    }
}

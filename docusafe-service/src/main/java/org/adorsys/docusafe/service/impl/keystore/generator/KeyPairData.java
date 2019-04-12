package org.adorsys.docusafe.service.impl.keystore.generator;

import lombok.Builder;
import lombok.Getter;
import org.adorsys.docusafe.service.api.keystore.types.CertificationResult;
import org.adorsys.docusafe.service.api.keystore.types.KeyPairEntry;
import org.adorsys.docusafe.service.api.keystore.types.SelfSignedKeyPairData;

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

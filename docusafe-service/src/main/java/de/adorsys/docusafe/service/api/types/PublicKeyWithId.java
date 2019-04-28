package de.adorsys.docusafe.service.api.types;

import de.adorsys.docusafe.service.api.keystore.types.KeyID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.security.PublicKey;

@Value
@Builder
public class PublicKeyWithId {

    @NonNull
    private final PublicKey publicKey;

    @NonNull
    private final KeyID publicKeyId;
}

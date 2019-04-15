package org.adorsys.docusafe.service.api.keystore.types;

import de.adorsys.common.utils.HexUtil;
import jdk.nashorn.internal.objects.annotations.Constructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.security.PublicKey;

@AllArgsConstructor
@Getter
@Setter
public class PublicKeyIDWithPublicKey {
    private KeyID keyID;
    private PublicKey publicKey;

    public PublicKeyIDWithPublicKey() {}

    @Override
    public String toString() {
        return "PublicKeyIDWithPublicKey{" +
                "keyID=" + keyID +
                ", publicKey.algorithm = " + publicKey.getAlgorithm() +
                ", publicKey.format = " + publicKey.getFormat() +
                ", publicKey.encoded = " + HexUtil.convertBytesToHexString(publicKey.getEncoded()) +
                '}';
    }
}

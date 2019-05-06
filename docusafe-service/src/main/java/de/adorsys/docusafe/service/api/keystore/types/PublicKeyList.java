package de.adorsys.docusafe.service.api.keystore.types;

import java.util.ArrayList;
import java.util.List;

public class PublicKeyList extends ArrayList<PublicKeyIDWithPublicKey> {
    public PublicKeyList() {}

    public PublicKeyList(List<PublicKeyIDWithPublicKey> list) {
        addAll(list);
    }
}

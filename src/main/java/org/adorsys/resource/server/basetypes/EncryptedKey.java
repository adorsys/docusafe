package org.adorsys.resource.server.basetypes;

import org.adorsys.resource.server.basetypes.adapter.EncryptedKeyRestAdapter;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Created by peter on 23.12.2017 at 18:34:54.
 */
@XmlJavaTypeAdapter(EncryptedKeyRestAdapter.class)
@XmlType
public class EncryptedKey extends BaseTypeByteArray {
    public EncryptedKey() {}

    public EncryptedKey(byte[] value) {
        super(value);
    }
}

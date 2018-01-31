package org.adorsys.documentsafe.layer02service.exceptions;

import org.adorsys.cryptoutils.exceptions.BaseException;

/**
 * Created by peter on 10.01.18 at 08:59.
 */
public class SymmetricEncryptionException extends BaseException {
    public SymmetricEncryptionException(String message) {
        super(message);
    }
}

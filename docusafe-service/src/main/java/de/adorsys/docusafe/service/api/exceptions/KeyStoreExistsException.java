package de.adorsys.docusafe.service.api.exceptions;

import de.adorsys.common.exceptions.BaseException;

/**
 * Created by peter on 20.01.18 at 17:09.
 */
public class KeyStoreExistsException extends BaseException {
    public KeyStoreExistsException(String message) {
        super(message);
    }
}

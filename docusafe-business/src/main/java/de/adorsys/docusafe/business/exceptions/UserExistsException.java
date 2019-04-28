package de.adorsys.docusafe.business.exceptions;

import de.adorsys.common.exceptions.BaseException;
import de.adorsys.docusafe.service.api.types.UserID;

public class UserExistsException extends BaseException {
    public UserExistsException(UserID userID) {
        super(userID.getValue());
    }
}

package org.adorsys.resource.server.basetypes;

import org.adorsys.resource.server.persistence.basetypes.BaseTypeString;

/**
 * Created by peter on 23.12.2017 at 17:22:42.
 */
public class UserID extends BaseTypeString {
	
    public UserID() {}

    public UserID(String value) {
        super(value);
    }
}

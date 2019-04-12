package org.adorsys.docusafe.service.impl.keystore.generator;


import lombok.Builder;
import lombok.Getter;
import org.adorsys.docusafe.service.api.keystore.types.SecretKeyEntry;

import javax.crypto.SecretKey;
import javax.security.auth.callback.CallbackHandler;

@Getter
public class SecretKeyData extends KeyEntryData implements SecretKeyEntry {

	private final SecretKey secretKey;
	private final String keyAlgo;

	@Builder
	private SecretKeyData(CallbackHandler passwordSource, String alias, SecretKey secretKey, String keyAlgo) {
		super(passwordSource, alias);
		this.secretKey = secretKey;
		this.keyAlgo = keyAlgo;
	}
}

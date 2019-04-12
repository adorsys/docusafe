package org.adorsys.docusafe.service.impl.keystore.generator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.adorsys.docusafe.service.api.keystore.types.KeyEntry;

import javax.security.auth.callback.CallbackHandler;

@Getter
@AllArgsConstructor
abstract class KeyEntryData implements KeyEntry {

	private final CallbackHandler passwordSource;
	
	private final String alias;
}

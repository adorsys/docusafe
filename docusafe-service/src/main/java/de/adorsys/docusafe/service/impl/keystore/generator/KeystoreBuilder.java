package de.adorsys.docusafe.service.impl.keystore.generator;

import de.adorsys.docusafe.service.api.keystore.types.KeyEntry;
import de.adorsys.docusafe.service.api.keystore.types.KeyStoreType;

import javax.security.auth.callback.CallbackHandler;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;

public class KeystoreBuilder {
	private KeyStoreType storeType;
	private String storeId;
	private Map<String, KeyEntry> keyEntries = new HashMap<>();
	
	public KeystoreBuilder withStoreType(KeyStoreType storeType) {
		this.storeType = storeType;
		return this;
	}
	public KeystoreBuilder withStoreId(String storeId) {
		this.storeId = storeId;
		return this;
	}
	public KeystoreBuilder withKeyEntry(KeyEntry keyEntry) {
		this.keyEntries.put(keyEntry.getAlias(), keyEntry);
		return this;
	}
	
	public byte[] build(CallbackHandler storePassSrc) {
		KeyStore ks = KeyStoreServiceImplBaseFunctions.newKeyStore(storeType);
		KeyStoreServiceImplBaseFunctions.fillKeyStore(ks, keyEntries.values());

		return KeyStoreServiceImplBaseFunctions.toByteArray(ks, storeId, storePassSrc);
	}

	public KeyStore build() {
		KeyStore ks = KeyStoreServiceImplBaseFunctions.newKeyStore(storeType);
		KeyStoreServiceImplBaseFunctions.fillKeyStore(ks, keyEntries.values());

		return ks;
	}
}

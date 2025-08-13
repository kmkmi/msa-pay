package com.msapay.membership.outbound.vault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;

@Component
@ConditionalOnProperty(name = "vault.enabled", havingValue = "true")
public class VaultAdapter {
    private final AESProvider encryptor;

    @Autowired
    public VaultAdapter(VaultTemplate vaultTemplate) {
        VaultKeyValueOperations ops = vaultTemplate.opsForKeyValue("kv-v1/data/encrypt", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
        String key = (String) ops.get("dbkey").getData().get("key");
        this.encryptor = new AESProvider(key);
    }

    public String encrypt(String plainText)  {
        try {
            return encryptor.encrypt(plainText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            return encryptor.decrypt(encryptedText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

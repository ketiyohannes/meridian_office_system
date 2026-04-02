package com.meridian.portal.security;

import com.meridian.portal.config.DataSecurityProperties;
import com.meridian.portal.exception.ValidationException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class SensitiveDataCryptoService {

    private static final int IV_SIZE = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public SensitiveDataCryptoService(DataSecurityProperties properties) {
        byte[] keyBytes = properties.getDataEncryptionKey().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new ValidationException("MERIDIAN_DATA_ENCRYPTION_KEY must be exactly 32 characters");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new ValidationException("Failed to encrypt sensitive value");
        }
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedBase64);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[IV_SIZE];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new ValidationException("Failed to decrypt sensitive value");
        }
    }

    public String mask(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        String value = plain.trim();
        int len = value.length();
        if (len <= 4) {
            return "*".repeat(len);
        }
        return "*".repeat(len - 4) + value.substring(len - 4);
    }
}

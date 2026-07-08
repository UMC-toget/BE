package com.example.toget.global.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 방식을 사용하여 엔티티 필드를 투명하게 암/복호화하는 JPA Attribute Converter.
 * DB 유출 시 개인 정보(계좌번호 등)가 평문으로 노출되는 것을 방지합니다.
 */
@Converter
@Component
public class AesGcmConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static SecretKey staticSecretKey;

    // 기본값 없음 — application.yaml에서 ENCRYPTION_KEY 환경변수를 강제한다 (공개 저장소에 실키가 남지 않도록)
    @Value("${encryption.key}")
    public void setKey(String keyString) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyString);
            if (keyBytes.length != 32) {
                keyBytes = keyString.getBytes();
            }
        } catch (IllegalArgumentException e) {
            keyBytes = keyString.getBytes();
        }

        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes (256 bits) long.");
        }
        staticSecretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        SecretKey key = staticSecretKey;
        if (key == null) {
            throw new IllegalStateException("Encryption key has not been initialized");
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes());

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting attribute", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        SecretKey key = staticSecretKey;
        if (key == null) {
            throw new IllegalStateException("Decryption key has not been initialized");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting attribute", e);
        }
    }
}

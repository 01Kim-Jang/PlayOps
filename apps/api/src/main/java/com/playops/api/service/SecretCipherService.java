package com.playops.api.service;

import com.playops.api.config.PlayOpsProperties;
import com.playops.api.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * AES-GCM 기반 프로젝트 시크릿(예: GitHub PAT) 암호화 서비스.
 * 키는 PLAYOPS_ENCRYPTION_KEY 환경변수 문자열을 SHA-256으로 해시하여 256비트 AES 키로 사용한다.
 */
@Service
public class SecretCipherService {

    private static final Logger log = LoggerFactory.getLogger(SecretCipherService.class);
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    // dev 전용 플레이스홀더. prod 프로필에서는 사용을 거부한다.
    private static final String DEV_PLACEHOLDER_KEY = "playops-dev-only-insecure-key-change-me";

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretCipherService(PlayOpsProperties properties, Environment environment) {
        String configuredKey = properties.security() != null ? properties.security().encryptionKey() : null;
        boolean prod = List.of(environment.getActiveProfiles()).contains("prod");

        if (configuredKey == null || configuredKey.isBlank()) {
            if (prod) {
                throw new IllegalStateException(
                        "PLAYOPS_ENCRYPTION_KEY must be set in the prod profile before storing project secrets (GitHub tokens 등)."
                );
            }
            log.warn("PLAYOPS_ENCRYPTION_KEY가 설정되지 않아 개발 전용 임시 키를 사용합니다. 로컬 개발 이외 환경에서는 반드시 설정하세요.");
            configuredKey = DEV_PLACEHOLDER_KEY;
        } else if (prod && DEV_PLACEHOLDER_KEY.equals(configuredKey)) {
            throw new IllegalStateException("PLAYOPS_ENCRYPTION_KEY에 개발용 플레이스홀더 값을 prod에서 사용할 수 없습니다.");
        }

        this.keySpec = deriveKey(configuredKey);
    }

    private SecretKeySpec deriveKey(String passphrase) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(passphrase.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호화 키 유도에 실패했습니다.", e);
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new ApiException(500, "시크릿 암호화에 실패했습니다: " + e.getMessage());
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new ApiException(500, "시크릿 복호화에 실패했습니다: " + e.getMessage());
        }
    }

    /** 로그/응답에 노출해도 안전하도록 시크릿 앞뒤 일부만 남기고 마스킹한다. */
    public static String mask(String secret) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "…" + secret.substring(secret.length() - 4);
    }
}

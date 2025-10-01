package com.alinesno.infra.data.scheduler.im.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * TODO 这个是AI生成的，我也不知道它为啥要这么处理，感觉先这样
 * AES-GCM 加解密工具（示例）
 *
 * 说明：
 * - 使用 AES/GCM/NoPadding，12 字节随机 IV，128-bit tag（16 字节）。
 * - 返回格式：Base64( iv || ciphertextWithTag )
 * - 密钥通过系统属性 "notification.crypto.key" 或环境变量 "NOTIFICATION_CRYPTO_KEY" 读取。
 * - 生产环境请使用 KMS 并妥善保护密钥。
 */
public class CryptoUtil {

    // 系统属性或环境变量键名
    private static final String SYS_PROP_KEY = "notification.crypto.key";
    private static final String ENV_KEY = "X3q9vGm2rY8b1Kz4sQe7Tn6uVw0yZcP4aB9dFjK6Lm0=";

    // AES-GCM 参数
    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128; // 16 bytes tag
    private static final int IV_LENGTH_BYTES = 12; // 推荐 12 bytes for GCM
    private static final SecureRandom RANDOM = new SecureRandom();

    // 默认开发用弱密钥（仅限开发/测试，生产请覆盖系统属性或环境变量）
    private static final String DEFAULT_DEV_KEY = "dev-please-change-this-key!";

    // 缓存的 SecretKey
    private static volatile SecretKey SECRET_KEY_SPEC = null;

    private CryptoUtil() {
        // util class
    }

    /**
     * 加密明文文本，返回 Base64 编码的 iv||ciphertextWithTag
     * @param plain 明文
     * @return Base64 字符串（不可为 null）
     */
    public static String encrypt(String plain) {
        if (plain == null) return null;
        try {
            SecretKey key = getSecretKey();
            byte[] iv = new byte[IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new RuntimeException("CryptoUtil encrypt error", ex);
        }
    }

    /**
     * 解密由 encrypt 生成的 Base64 字符串（iv || ciphertextWithTag）
     * @param base64 Base64 编码字符串
     * @return 明文
     */
    public static String decrypt(String base64) {
        if (base64 == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(base64);
            if (all.length < IV_LENGTH_BYTES + 1) {
                throw new IllegalArgumentException("invalid ciphertext");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH_BYTES);
            int cipherLen = all.length - IV_LENGTH_BYTES;
            byte[] cipherText = new byte[cipherLen];
            System.arraycopy(all, IV_LENGTH_BYTES, cipherText, 0, cipherLen);

            SecretKey key = getSecretKey();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("CryptoUtil decrypt error", ex);
        }
    }

    /**
     * 获取或创建 SecretKey（基于用户提供的字符串派生出 256-bit AES key）
     */
    private static SecretKey getSecretKey() {
        if (SECRET_KEY_SPEC == null) {
            synchronized (CryptoUtil.class) {
                if (SECRET_KEY_SPEC == null) {
                    String k = System.getProperty(SYS_PROP_KEY);
                    if (k == null || k.trim().isEmpty()) {
                        k = System.getenv(ENV_KEY);
                    }
                    if (k == null || k.trim().isEmpty()) {
                        k = DEFAULT_DEV_KEY;
                        // 开发环境下可打印提示（注意不要在生产打印真实 key）
                        System.err.println("Warning: using default CryptoUtil key. Set system property '" +
                                SYS_PROP_KEY + "' or env '" + ENV_KEY + "' to override.");
                    }
                    SECRET_KEY_SPEC = deriveKey(k);
                }
            }
        }
        return SECRET_KEY_SPEC;
    }

    /**
     * 使用 SHA-256 对输入做散列，并使用前 32 字节作为 AES-256 密钥
     * @param keyMaterial 任意长度字符串
     * @return SecretKeySpec (AES)
     */
    private static SecretKey deriveKey(String keyMaterial) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(keyMaterial.getBytes(StandardCharsets.UTF_8));
            // 使用 256-bit key
            byte[] keyBytes = new byte[32];
            System.arraycopy(hash, 0, keyBytes, 0, 32);
            return new SecretKeySpec(keyBytes, AES);
        } catch (Exception ex) {
            throw new RuntimeException("deriveKey error", ex);
        }
    }

    // 可选：生成一个随机 256-bit key（字符串形式，base64），用于 KMS 或初始化配置
    public static String generateRandomKeyBase64() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance(AES);
            kg.init(256);
            SecretKey sk = kg.generateKey();
            return Base64.getEncoder().encodeToString(sk.getEncoded());
        } catch (Exception ex) {
            throw new RuntimeException("generateRandomKeyBase64 error", ex);
        }
    }
}
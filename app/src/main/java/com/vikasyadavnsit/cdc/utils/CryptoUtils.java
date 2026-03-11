package com.vikasyadavnsit.cdc.utils;


import android.os.Build;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256; // AES-256

    /**
     * Generates a new random AES key.
     *
     * @return A newly generated {@link SecretKey} (AES-256), or {@code null} if generation fails.
     */
    public static SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE);
            return keyGen.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Encrypts the provided data using AES/CBC/PKCS5Padding.
     *
     * @param key  AES secret key.
     * @param iv   Initialization vector bytes (must be 16 bytes for AES-CBC).
     * @param data Plaintext bytes to encrypt.
     * @return Ciphertext bytes.
     * @throws Exception If the cipher cannot be initialized or encryption fails.
     */
    public static byte[] encrypt(SecretKey key, byte[] iv, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts the provided AES/CBC/PKCS5Padding ciphertext.
     *
     * @param key           AES secret key.
     * @param iv            Initialization vector bytes (must match the IV used for encryption).
     * @param encryptedData Ciphertext bytes to decrypt.
     * @return Plaintext bytes.
     * @throws Exception If the cipher cannot be initialized or decryption fails.
     */
    public static byte[] decrypt(SecretKey key, byte[] iv, byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Encodes a {@link SecretKey} into a string suitable for storage.
     *
     * @param key Secret key to encode.
     * @return Base64-encoded key bytes on Android O+; otherwise a raw byte-string representation.
     */
    public static String keyToString(SecretKey key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(key.getEncoded());
        }
        return new String(key.getEncoded());
    }

    /**
     * Decodes a previously encoded key string into a {@link SecretKey}.
     *
     * @param keyStr Base64-encoded key string.
     * @return Decoded AES {@link SecretKey}.
     */
    public static SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            decodedKey = Base64.getDecoder().decode(keyStr);
        }
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    /**
     * Generates a new random initialization vector (IV) for AES-CBC.
     *
     * @return A 16-byte IV.
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    /**
     * Encodes an IV byte array into a string suitable for storage.
     *
     * @param iv IV bytes.
     * @return Base64-encoded IV bytes on Android O+; otherwise a raw byte-string representation.
     */
    public static String ivToString(byte[] iv) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(iv);
        }
        return new String(iv);
    }

    /**
     * Decodes a previously encoded IV string into its byte array form.
     *
     * @param ivStr Base64-encoded IV string.
     * @return Decoded IV bytes.
     */
    public static byte[] stringToIV(String ivStr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getDecoder().decode(ivStr);
        }
        return ivStr.getBytes();
    }

    /**
     * Optionally encrypts a string based on the {@link FileMap} configuration.
     *
     * <p>If {@code fileMap.encrypted == true} and the platform supports {@link Base64} (Android O+),
     * this encrypts the UTF-8 bytes using the static AES key/IV configured in {@link AppConstants}
     * and returns the Base64-encoded ciphertext. Otherwise, it returns the original data.</p>
     *
     * @param fileMap Target file type configuration controlling whether encryption is applied.
     * @param data    Plaintext string to (optionally) encrypt.
     * @return Encrypted Base64 string when encryption is enabled; otherwise the original {@code data}.
     * @throws Exception If encryption fails when enabled.
     */
    public static String getEncryptedData(FileMap fileMap, String data) throws Exception {
        if (fileMap.isEncrypted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            byte[] encryptedData = encrypt(CryptoUtils.stringToKey(AppConstants.CRYPTO_AES_SECRET_KEY),
                    CryptoUtils.stringToIV(AppConstants.CRYTPO_AES_IV), data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        }
        return data;
    }

    /**
     * One-off helper used to generate a key and IV for manual copy/paste into configuration.
     *
     * <p>Not used by production flows.</p>
     */
    private void keyGenerationOneTime() {
        SecretKey key = CryptoUtils.generateKey();
        byte[] iv = CryptoUtils.generateIV();

        // Convert key and IV to strings for storage
        String keyStr = CryptoUtils.keyToString(key);
        String ivStr = CryptoUtils.ivToString(iv);
    }
}

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

    // Generate a new AES key
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

    // Encrypt data
    public static byte[] encrypt(SecretKey key, byte[] iv, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data);
    }

    // Decrypt data
    public static byte[] decrypt(SecretKey key, byte[] iv, byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(encryptedData);
    }

    // Convert SecretKey to String
    public static String keyToString(SecretKey key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(key.getEncoded());
        }
        return new String(key.getEncoded());
    }

    // Convert String to SecretKey
    public static SecretKey stringToKey(String keyStr) {
        byte[] decodedKey = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            decodedKey = Base64.getDecoder().decode(keyStr);
        }
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    // Generate a new IV
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    // Convert IV to String
    public static String ivToString(byte[] iv) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(iv);
        }
        return new String(iv);
    }

    // Convert String to IV
    public static byte[] stringToIV(String ivStr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getDecoder().decode(ivStr);
        }
        return ivStr.getBytes();
    }

    public static String getEncryptedData(FileMap fileMap, String data) throws Exception {
        if (fileMap.isEncrypted() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            byte[] encryptedData = encrypt(CryptoUtils.stringToKey(AppConstants.CRYPTO_AES_SECRET_KEY),
                    CryptoUtils.stringToIV(AppConstants.CRYTPO_AES_IV), data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedData);
        }
        return data;
    }

    private void keyGenerationOneTime() {
        SecretKey key = CryptoUtils.generateKey();
        byte[] iv = CryptoUtils.generateIV();

        // Convert key and IV to strings for storage
        String keyStr = CryptoUtils.keyToString(key);
        String ivStr = CryptoUtils.ivToString(iv);
    }
}

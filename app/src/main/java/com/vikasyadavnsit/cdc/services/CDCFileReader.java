package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateDirectory;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateFile;

import android.os.Environment;
import android.util.Log;

import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.CryptoUtils;
import com.vikasyadavnsit.cdc.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class CDCFileReader {

    public static void readAndCreateTemporaryFile(FileMap fileMap) {
        readAndProcess(fileMap, false, true);
    }

    public static void readAndPrint(FileMap fileMap) {
        readAndProcess(fileMap, true, false);
    }

    public static void readAndPrintAndWrite(FileMap fileMap) {
        readAndProcess(fileMap, true, true);
    }

    public static void readAndProcess(FileMap fileMap, boolean print, boolean writetoNewFile) {
        try {
            File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
            File file = new File(directory, fileMap.getFileName());
            if (checkAndCreateDirectory(directory) || checkAndCreateFile(file)) {
                return;
            }
            //Deleting Temporary File if it exists
            deleteFileIfItExists(FileMap.TEMPORARY_FILE);

            try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (fileMap.isEncrypted()) {
                        line = decryptIfNeeded(line);
                    }
                    printOrWrite(fileMap, print, writetoNewFile, line);
                }
            }
        } catch (
                Exception e) {
            Log.e("FileReader", "Error while reading to file", e);
        }
    }


    public static String decryptIfNeeded(String line) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                byte[] encryptedData = Base64.getDecoder().decode(line);
                byte[] decryptedData = CryptoUtils.decrypt(CryptoUtils.stringToKey(AppConstants.CRYPTO_AES_SECRET_KEY), CryptoUtils.stringToIV(AppConstants.CRYTPO_AES_IV), encryptedData);
                return new String(decryptedData, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            Log.e("FileReader", "Error decrypting line: " + line, e);
            return line; // Return original line on decryption failure
        }
        return null;
    }

    private static void printOrWrite(FileMap fileMap, boolean print, boolean writetoNewFile, String data) {
        if (print) {
            Log.i("Decrypted Data", data);
        }
        if (writetoNewFile) {
            FileUtils.appendDataToFile(FileMap.TEMPORARY_FILE, data);
        }
    }

    private static void deleteFileIfItExists(FileMap fileMap) {
        File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
        File file = new File(directory, fileMap.getFileName());
        if (directory.exists() && file.exists()) {
            file.delete();
        }
    }


}


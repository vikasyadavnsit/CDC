package com.vikasyadavnsit.cdc.services;

import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateDirectory;
import static com.vikasyadavnsit.cdc.utils.CommonUtil.checkAndCreateFile;

import android.os.Environment;
import com.vikasyadavnsit.cdc.constants.AppConstants;
import com.vikasyadavnsit.cdc.enums.FileMap;
import com.vikasyadavnsit.cdc.utils.CryptoUtils;
import com.vikasyadavnsit.cdc.utils.FileUtils;
import com.vikasyadavnsit.cdc.utils.LoggerUtils;

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
        LoggerUtils.d("CDCFileReader", "Processing FileMap: " + fileMap.name());
        try {
            File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
            File file = new File(directory, fileMap.getFileName());
            if (checkAndCreateDirectory(directory) || checkAndCreateFile(file)) {
                LoggerUtils.w("CDCFileReader", "File or directory could not be created/found: " + file.getAbsolutePath());
                return;
            }
            //Deleting Temporary File if it exists
            deleteFileIfItExists(FileMap.TEMPORARY_FILE);

            LoggerUtils.i("CDCFileReader", "Reading file: " + file.getAbsolutePath());
            try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    if (fileMap.isEncrypted()) {
                        line = decryptIfNeeded(line);
                    }
                    printOrWrite(fileMap, print, writetoNewFile, line);
                }
                LoggerUtils.d("CDCFileReader", "Finished reading " + lineCount + " lines");
            }
        } catch (
                Exception e) {
            LoggerUtils.e("CDCFileReader", "Error while reading to file :: " + e.getMessage());
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
            LoggerUtils.e("CDCFileReader", "Decryption failed for line: " + e.getMessage());
            return line; // Return original line on decryption failure
        }
        return null;
    }

    private static void printOrWrite(FileMap fileMap, boolean print, boolean writetoNewFile, String data) {
        if (print) {
            LoggerUtils.i("Decrypted Data", data);
        }
        if (writetoNewFile) {
            FileUtils.appendDataToFile(FileMap.TEMPORARY_FILE, data);
        }
    }

    public static java.util.List<String> readAllLines(FileMap fileMap) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        try {
            File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
            File file = new File(directory, fileMap.getFileName());
            if (!directory.exists() || !file.exists()) {
                return lines;
            }

            try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (fileMap.isEncrypted()) {
                        line = decryptIfNeeded(line);
                    }
                    if (line != null) lines.add(line);
                }
            }
        } catch (Exception e) {
            LoggerUtils.e("FileReader", "Error while reading all lines :: " + e.getMessage());
        }
        return lines;
    }

    public static void deleteFileIfItExists(FileMap fileMap) {
        File directory = Environment.getExternalStoragePublicDirectory(fileMap.getDirectoryPath());
        File file = new File(directory, fileMap.getFileName());
        if (directory.exists() && file.exists()) {
            file.delete();
        }
    }


}


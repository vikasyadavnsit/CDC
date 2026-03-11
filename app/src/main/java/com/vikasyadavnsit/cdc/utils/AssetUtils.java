package com.vikasyadavnsit.cdc.utils;


import android.content.Context;
import android.content.res.AssetManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AssetUtils {

    /**
     * Loads a JSON file bundled in the application's assets folder into a single {@link String}.
     *
     * <p>This helper opens the named asset via {@link AssetManager#open(String)}, reads it line-by-line,
     * concatenates the lines, and returns the resulting string. The stream is closed in a {@code finally}
     * block.</p>
     *
     * @param context  Android {@link Context} used to access the app {@link AssetManager}.
     * @param fileName Asset file name/path relative to the {@code assets/} directory (for example,
     *                 {@code "android_version_mappings.json"}).
     * @return The full JSON contents as a string, or {@code null} if an {@link IOException} occurs.
     */
    public static String loadJSONFromAsset(Context context, String fileName) {
        String json = null;
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            json = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return json;
    }
}


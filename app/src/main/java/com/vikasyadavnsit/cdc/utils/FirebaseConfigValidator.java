package com.vikasyadavnsit.cdc.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FirebaseConfigValidator {

    public static boolean isValidStructure(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("project_info") || !root.has("client")) {
                return false;
            }

            JsonObject projectInfo = root.getAsJsonObject("project_info");
            if (!projectInfo.has("project_id") || !projectInfo.has("firebase_url")) {
                return false;
            }

            JsonArray clients = root.getAsJsonArray("client");
            if (clients.size() == 0) {
                return false;
            }

            JsonObject firstClient = clients.get(0).getAsJsonObject();
            if (!firstClient.has("client_info") || !firstClient.has("api_key")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPackageNameFromJson(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray clients = root.getAsJsonArray("client");
            if (clients.size() > 0) {
                JsonObject clientInfo = clients.get(0).getAsJsonObject().getAsJsonObject("client_info");
                return clientInfo.getAsJsonObject("android_client_info").get("package_name").getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean validateAgainstReference(String inputJson, String referenceJson) {
        try {
            JsonObject inputRoot = JsonParser.parseString(inputJson).getAsJsonObject();
            JsonObject refRoot = JsonParser.parseString(referenceJson).getAsJsonObject();

            return compareKeys(inputRoot, refRoot);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean compareKeys(JsonObject input, JsonObject ref) {
        for (String key : ref.keySet()) {
            if (!input.has(key)) {
                return false;
            }
            JsonElement inputVal = input.get(key);
            JsonElement refVal = ref.get(key);

            if (refVal.isJsonObject()) {
                if (!inputVal.isJsonObject() || !compareKeys(inputVal.getAsJsonObject(), refVal.getAsJsonObject())) {
                    return false;
                }
            } else if (refVal.isJsonArray()) {
                if (!inputVal.isJsonArray()) {
                    return false;
                }
                // For simplicity, we just check the first element of the array if it exists in reference.
                if (refVal.getAsJsonArray().size() > 0 && inputVal.getAsJsonArray().size() > 0) {
                    JsonElement firstRef = refVal.getAsJsonArray().get(0);
                    JsonElement firstInput = inputVal.getAsJsonArray().get(0);
                    if (firstRef.isJsonObject() && firstInput.isJsonObject()) {
                        if (!compareKeys(firstInput.getAsJsonObject(), firstRef.getAsJsonObject())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    public static boolean containsInjection(String json) {
        // Simple check for common injection patterns if any, though JSON parser handles most.
        String lower = json.toLowerCase();
        return lower.contains("<script") || lower.contains("javascript:");
    }
}

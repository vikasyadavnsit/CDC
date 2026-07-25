package com.vikasyadavnsit.cdc.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.data.SpendingEntry;
import com.vikasyadavnsit.cdc.enums.SpendingCategory;

import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpendingCategoryStore {

    private static final String PREFS_NAME = "CDC_SPENDING_PREFS";
    private static final String KEY_CATEGORIES = "category_map";
    private static final String KEY_TYPES = "type_map";
    private static final String KEY_DELETED = "deleted_ids";
    private static final Gson gson = new Gson();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    public static Map<String, SpendingCategory> loadAll(Context context) {
        String json = prefs(context).getString(KEY_CATEGORIES, null);
        if (json == null) return new HashMap<>();
        Map<String, String> raw = gson.fromJson(json, STRING_MAP_TYPE);
        Map<String, SpendingCategory> result = new HashMap<>();
        if (raw != null) {
            for (Map.Entry<String, String> e : raw.entrySet()) {
                try { result.put(e.getKey(), SpendingCategory.valueOf(e.getValue())); }
                catch (IllegalArgumentException ignored) {}
            }
        }
        return result;
    }

    public static Map<String, String> loadTypes(Context context) {
        String json = prefs(context).getString(KEY_TYPES, null);
        if (json == null) return new HashMap<>();
        Map<String, String> result = gson.fromJson(json, STRING_MAP_TYPE);
        return result != null ? result : new HashMap<>();
    }

    public static Set<String> loadDeleted(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_DELETED, new HashSet<>()));
    }

    public static void assign(Context context, SpendingEntry entry, SpendingCategory category, String type) {
        entry.category = category;
        entry.type = type;

        Map<String, SpendingCategory> cats = loadAll(context);
        cats.put(entry.id, category);
        saveCategoriesRaw(context, cats);

        Map<String, String> types = loadTypes(context);
        types.put(entry.id, type);
        prefs(context).edit().putString(KEY_TYPES, gson.toJson(types)).apply();

        FirebaseUtils.saveSpendingEntry(entry);
    }

    public static void deleteEntry(Context context, String smsId) {
        Set<String> deleted = loadDeleted(context);
        deleted.add(smsId);
        prefs(context).edit().putStringSet(KEY_DELETED, deleted).apply();
    }

    public static void syncFromFirebase(Context context) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        syncYear(context, currentYear);
        syncYear(context, currentYear - 1);
    }

    private static void syncYear(Context context, int year) {
        FirebaseUtils.getSpendingDataForYear(year, (categoryMap, typeMap) -> {
            if (categoryMap.isEmpty() && typeMap.isEmpty()) return;

            Map<String, SpendingCategory> localCats = loadAll(context);
            for (Map.Entry<String, String> e : categoryMap.entrySet()) {
                try { localCats.put(e.getKey(), SpendingCategory.valueOf(e.getValue())); }
                catch (IllegalArgumentException ignored) {}
            }
            saveCategoriesRaw(context, localCats);

            Map<String, String> localTypes = loadTypes(context);
            localTypes.putAll(typeMap);
            prefs(context).edit().putString(KEY_TYPES, gson.toJson(localTypes)).apply();
        });
    }

    private static void saveCategoriesRaw(Context context, Map<String, SpendingCategory> map) {
        Map<String, String> raw = new HashMap<>();
        for (Map.Entry<String, SpendingCategory> e : map.entrySet()) {
            raw.put(e.getKey(), e.getValue().name());
        }
        prefs(context).edit().putString(KEY_CATEGORIES, gson.toJson(raw)).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

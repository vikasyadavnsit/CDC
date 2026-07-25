package com.vikasyadavnsit.cdc.database.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vikasyadavnsit.cdc.data.TodoItem;
import com.vikasyadavnsit.cdc.utils.FirebaseUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TodoRepository {

    private static final String PREFS_NAME = "CDC_TODO_PREFS";
    private static final String KEY = "todo_list";
    private static final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<TodoItem>>() {}.getType();

    public static List<TodoItem> getAll(Context context) {
        String json = prefs(context).getString(KEY, null);
        if (json == null) return new ArrayList<>();
        try {
            List<TodoItem> items = gson.fromJson(json, LIST_TYPE);
            return items != null ? items : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static void saveAll(Context context, List<TodoItem> items) {
        prefs(context).edit().putString(KEY, gson.toJson(items)).apply();
        FirebaseUtils.saveTodos(items);
    }

    public static void syncFromFirebase(Context context) {
        FirebaseUtils.getTodos(items -> {
            if (items == null || items.isEmpty()) return;
            prefs(context).edit().putString(KEY, gson.toJson(items)).apply();
        });
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}

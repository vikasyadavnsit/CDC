package com.vikasyadavnsit.cdc.enums;

import android.content.Context;

import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.util.function.BiConsumer;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ApplicationInputActions {

    FIREBASE_CREATE_USER(
            true,
            "Please enter your name for a custom experience",
            input -> {
                if (input.isEmpty())                      return "Name cannot be empty";
                if (input.length() < 2)                   return "Name must be at least 2 characters";
                if (!input.matches("[a-zA-Z ]+"))          return "Name can only contain letters and spaces";
                if (!Character.isLetter(input.charAt(0))) return "Name must start with a letter";
                return null;
            },
            (context, data) -> {
                FirebaseUtils.createUser(data.toString());
                FirebaseUtils.getAppTriggerSettingsData();
                SharedPreferenceUtils.setFirstLaunchCompleted(context);
            }
    );

    boolean enabled;
    String description;
    Function<String, String> validator;
    BiConsumer<Context, Object> biConsumer;
}

package com.vikasyadavnsit.cdc.enums;

import android.content.Context;

import com.vikasyadavnsit.cdc.utils.FirebaseUtils;
import com.vikasyadavnsit.cdc.utils.SharedPreferenceUtils;

import java.util.function.BiConsumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ApplicationInputActions {

    FIREBASE_CREATE_USER(true, (context, data) -> {
        FirebaseUtils.checkAndCreateUser(data.toString());
        FirebaseUtils.getAppTriggerSettingsData();
        SharedPreferenceUtils.setFirstLaunchCompleted(context);
    });

    boolean enabled;
    BiConsumer<Context, Object> biConsumer;
}

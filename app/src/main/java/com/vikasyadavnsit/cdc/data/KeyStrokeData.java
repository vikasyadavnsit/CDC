package com.vikasyadavnsit.cdc.data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@com.google.firebase.database.IgnoreExtraProperties
public class KeyStrokeData {
    private String text;
    private String appPackage;
    private String timestamp;
    private boolean typed; // true if from an editable field, false if captured from UI/Suggestions
}

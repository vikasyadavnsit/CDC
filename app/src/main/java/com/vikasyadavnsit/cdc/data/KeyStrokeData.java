package com.vikasyadavnsit.cdc.data;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class KeyStrokeData {
    private String text;
    private String appPackage;
    private String  timestamp;

}

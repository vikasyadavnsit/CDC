package com.vikasyadavnsit.cdc.data;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class KeyStrokeData {
    private String text;
    private String appPackage;
    private String timestamp;

    @Override
    public String toString() {
        return timestamp + "::" + appPackage + "->  " + text;
    }

}

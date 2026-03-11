package com.vikasyadavnsit.cdc.data;

import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class NotificationData {

    private Map<String, Object> extras;
    private String packageName;
    private String timestamp;

}


package com.vikasyadavnsit.cdc.data;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NotificationData {

    private Map<String, Object> extras;
    private String packageName;
    private String timestamp;

}

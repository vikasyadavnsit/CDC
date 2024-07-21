package com.vikasyadavnsit.cdc.data;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class NotificationData {

    private String title;
    private String content;
    private String bigText;
    private String subText;
    private String summaryText;
    private String infoText;
    private String conversationTitle;
    private String packageName;
    private String timestamp;


}

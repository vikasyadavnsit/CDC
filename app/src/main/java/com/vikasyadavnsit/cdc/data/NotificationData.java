package com.vikasyadavnsit.cdc.data;

import java.util.LinkedList;
import java.util.List;

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
    private String[] extras;
    private String packageName;
    private String timestamp;


}

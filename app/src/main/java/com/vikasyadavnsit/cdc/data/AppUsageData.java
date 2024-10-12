package com.vikasyadavnsit.cdc.data;

import com.vikasyadavnsit.cdc.enums.AppStatus;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class AppUsageData {
    private String packageName;
    private Long start;
    private Long end;
    private AppStatus status;

}

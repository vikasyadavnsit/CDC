package com.vikasyadavnsit.cdc.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoggingLevel {

    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4);

    public final int value;
}

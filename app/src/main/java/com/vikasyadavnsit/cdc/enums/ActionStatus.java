package com.vikasyadavnsit.cdc.enums;

import lombok.Getter;

@Getter
public enum ActionStatus {

    IDLE,
    PREPARE,
    START,
    STOP,
    CANCEL
}

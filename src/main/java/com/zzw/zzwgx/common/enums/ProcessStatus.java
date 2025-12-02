package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 工序状态枚举
 */
@Getter
public enum ProcessStatus {
    NOT_STARTED("NOT_STARTED", "未开始"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;

    ProcessStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProcessStatus fromCode(String code) {
        for (ProcessStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}


package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatus {
    PENDING("PENDING", "待完成"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String desc;

    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskStatus fromCode(String code) {
        for (TaskStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}


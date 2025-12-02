package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 项目状态枚举
 */
@Getter
public enum ProjectStatus {
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成"),
    PAUSED("PAUSED", "已暂停");

    private final String code;
    private final String desc;

    ProjectStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProjectStatus fromCode(String code) {
        for (ProjectStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}


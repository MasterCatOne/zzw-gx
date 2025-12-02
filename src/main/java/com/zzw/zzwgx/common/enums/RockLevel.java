package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 围岩等级枚举
 */
@Getter
public enum RockLevel {
    LEVEL_I("LEVEL_I", "I级"),
    LEVEL_II("LEVEL_II", "II级");

    private final String code;
    private final String desc;

    RockLevel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RockLevel fromCode(String code) {
        for (RockLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        return null;
    }
}


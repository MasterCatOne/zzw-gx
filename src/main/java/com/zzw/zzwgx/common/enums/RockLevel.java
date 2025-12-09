package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 围岩等级枚举
 */
@Getter
public enum RockLevel {
    LEVEL_I("IVa级（人工钻爆）", "I级"),
    LEVEL_II("IVb级（机器钻爆）", "II级");

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


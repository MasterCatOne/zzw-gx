package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    ADMIN("ADMIN", "管理员"),
    WORKER("WORKER", "施工人员"),
    SYSTEMADMIN("SYSTEMWORKER", "系统管理员");

    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return null;
    }
}


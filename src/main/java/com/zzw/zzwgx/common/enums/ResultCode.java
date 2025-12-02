package com.zzw.zzwgx.common.enums;

import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    
    // 认证相关 1000-1099
    UNAUTHORIZED(1001, "未登录或token已过期"),
    FORBIDDEN(1002, "没有权限"),
    LOGIN_ERROR(1003, "用户名或密码错误"),
    TOKEN_INVALID(1004, "token无效"),
    
    // 参数相关 1100-1199
    PARAM_ERROR(1100, "参数错误"),
    PARAM_MISSING(1101, "参数缺失"),
    
    // 业务相关 2000-2999
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_DISABLED(2002, "用户已被禁用"),
    USERNAME_ALREADY_EXISTS(2003, "用户名已存在"),
    PROJECT_NOT_FOUND(2003, "项目不存在"),
    CYCLE_NOT_FOUND(2004, "循环不存在"),
    PROCESS_NOT_FOUND(2005, "工序不存在"),
    TASK_NOT_FOUND(2006, "任务不存在"),
    TASK_ALREADY_STARTED(2007, "任务已开始"),
    TASK_ALREADY_COMPLETED(2008, "任务已完成"),
    TASK_NOT_STARTED(2009, "任务未开始"),
    PREVIOUS_PROCESS_NOT_COMPLETED(2010, "上一工序未完成"),
    OVERTIME_REASON_REQUIRED(2011, "超时原因必填"),
    TEMPLATE_NOT_FOUND(2012, "工序模板不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}


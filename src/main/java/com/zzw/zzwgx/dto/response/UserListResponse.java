package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表响应DTO
 */
@Data
@Schema(description = "用户列表响应")
public class UserListResponse {
    
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    @Schema(description = "用户账号", example = "worker01")
    private String username;
    
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "用户状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "用户角色列表", example = "[\"WORKER\"]")
    private List<String> roles;

    @Schema(description = "创建时间", example = "2024-01-01 10:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 10:00:00")
    private LocalDateTime updateTime;
}


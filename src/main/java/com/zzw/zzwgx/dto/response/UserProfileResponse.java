package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户个人信息响应DTO
 */
@Data
@Schema(description = "用户个人信息响应")
public class UserProfileResponse {
    
    @Schema(description = "用户ID", example = "1")
    private Long id;
    
    @Schema(description = "用户名", example = "admin")
    private String username;
    
    @Schema(description = "真实姓名", example = "管理员")
    private String realName;
    
    @Schema(description = "角色（兼容字段）", example = "ADMIN")
    private String role;
    
    @Schema(description = "角色列表", example = "[\"ADMIN\"]")
    private List<String> roles;
    
    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;
    
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}


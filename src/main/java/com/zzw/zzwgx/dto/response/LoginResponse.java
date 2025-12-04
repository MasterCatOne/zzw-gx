package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 登录响应DTO
 */
@Data
@Schema(description = "登录响应")
public class LoginResponse {
    
    @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String token;
    
    @Schema(description = "用户信息")
    private UserInfo userInfo;
    
    @Data
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID", example = "1")
        private Long id;
        
        @Schema(description = "用户名", example = "admin")
        private String username;
        
        @Schema(description = "真实姓名", example = "管理员")
        private String realName;
        
        @Schema(description = "角色（兼容字段）", example = "ADMIN")
        private String role;
        
        @Schema(description = "角色列表", example = "[\"ADMIN\", \"SYSTEM_ADMIN\"]")
        private List<String> roles;
    }
}


package com.zzw.zzwgx.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
@Schema(description = "用户登录请求参数")
public class LoginRequest {
    
    @Schema(description = "用户账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "admin01")
    @NotBlank(message = "账号不能为空")
    private String username;
    
    @Schema(description = "用户密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}


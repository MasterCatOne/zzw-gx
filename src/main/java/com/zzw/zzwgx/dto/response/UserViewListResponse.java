package com.zzw.zzwgx.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户列表响应DTO
 */
@Data
@Schema(description = "选择用户响应")
public class UserViewListResponse {

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "用户账号", example = "worker01")
    private String username;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;
}

package com.zzw.zzwgx.dto.response;

import lombok.Data;

/**
 * 用户个人信息响应DTO
 */
@Data
public class UserProfileResponse {
    
    private Long id;
    private String username;
    private String realName;
    private String role;
    private String idCard;
    private String phone;
}


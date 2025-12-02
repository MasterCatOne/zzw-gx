package com.zzw.zzwgx.service;

import com.zzw.zzwgx.dto.request.LoginRequest;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.response.LoginResponse;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 用户注册并自动登录
     */
    LoginResponse registerAndLogin(RegisterRequest request);
    
    /**
     * 用户退出登录
     */
    void logout();
}


package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.LoginRequest;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.response.LoginResponse;
import com.zzw.zzwgx.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@Tag(name = "认证管理", description = "用户登录、退出等认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @Operation(summary = "用户登录", description = "管理员和施工人员登录接口")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求，用户名: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
    
    @Operation(summary = "用户注册", description = "新用户注册接口，默认注册为施工人员角色，注册成功后自动登录")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求，用户名: {}, 姓名: {}", request.getUsername(), request.getRealName());
        LoginResponse response = authService.registerAndLogin(request);
        return Result.success(response);
    }
    
    @Operation(summary = "退出登录", description = "用户退出登录接口")
    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }
}


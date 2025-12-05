package com.zzw.zzwgx.controller;

import com.zzw.zzwgx.common.Result;
import com.zzw.zzwgx.dto.request.LoginRequest;
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
    
    @Operation(summary = "用户登录", description = "管理员和施工人员登录接口。登录成功后返回JWT令牌和用户信息（包括用户ID、用户名、真实姓名、角色列表等）。")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求，用户名: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
    
    @Operation(summary = "退出登录", description = "用户退出登录接口。注意：由于使用JWT令牌，实际退出需要前端清除本地存储的令牌。")
    @PostMapping("/logout")
    public Result<?> logout() {
        authService.logout();
        return Result.success();
    }
}


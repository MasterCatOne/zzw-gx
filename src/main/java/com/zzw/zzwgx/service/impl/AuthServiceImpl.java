package com.zzw.zzwgx.service.impl;

import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.LoginRequest;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.response.LoginResponse;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.security.JwtUtil;
import com.zzw.zzwgx.security.SecurityUser;
import com.zzw.zzwgx.service.AuthService;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("处理用户登录，用户名: {}", request.getUsername());
        
        try {
            // 执行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            // 设置认证信息到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            
            // 生成JWT token
            String token = jwtUtil.generateToken(
                    securityUser.getUserId(),
                    securityUser.getUsername(),
                    securityUser.getRole()
            );
            
            // 获取用户信息
            User user = userService.getById(securityUser.getUserId());
            
            // 构建响应
            LoginResponse response = buildLoginResponse(token, user);
            
            log.info("用户登录成功，用户ID: {}, 用户名: {}, 角色: {}", 
                    user.getId(), user.getUsername(), user.getRole());
            return response;
        } catch (BadCredentialsException e) {
            log.warn("用户登录失败，用户名: {}, 原因: 用户名或密码错误", request.getUsername());
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }
    }
    
    @Override
    public LoginResponse registerAndLogin(RegisterRequest request) {
        log.info("处理用户注册并自动登录，用户名: {}, 姓名: {}", request.getUsername(), request.getRealName());
        
        // 注册用户
        User user = userService.register(request);
        
        try {
            // 注册成功后自动登录
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            // 设置认证信息到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            
            // 生成JWT token
            String token = jwtUtil.generateToken(
                    securityUser.getUserId(),
                    securityUser.getUsername(),
                    securityUser.getRole()
            );
            
            // 构建响应
            LoginResponse response = buildLoginResponse(token, user);
            
            log.info("用户注册并登录成功，用户ID: {}, 用户名: {}, 角色: {}", 
                    user.getId(), user.getUsername(), user.getRole());
            return response;
        } catch (BadCredentialsException e) {
            log.error("用户注册后自动登录失败，用户名: {}, 原因: {}", request.getUsername(), e.getMessage());
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }
    }
    
    @Override
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            log.info("用户退出登录，用户ID: {}, 用户名: {}", 
                    securityUser.getUserId(), securityUser.getUsername());
        }
        SecurityContextHolder.clearContext();
    }
    
    /**
     * 构建登录响应
     */
    private LoginResponse buildLoginResponse(String token, User user) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setRealName(user.getRealName());
        userInfo.setRole(user.getRole());
        response.setUserInfo(userInfo);
        
        return response;
    }
}


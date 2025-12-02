package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.enums.UserRole;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.UserMapper;
import com.zzw.zzwgx.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final PasswordEncoder passwordEncoder;
    
    public UserServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public User getByUsername(String username) {
        log.debug("根据用户名查询用户，用户名: {}", username);
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user != null) {
            log.debug("查询到用户，用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), user.getRole());
        } else {
            log.debug("未查询到用户，用户名: {}", username);
        }
        return user;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(RegisterRequest request) {
        log.info("开始注册用户，用户名: {}, 姓名: {}", request.getUsername(), request.getRealName());
        
        // 检查用户名是否已存在
        User existingUser = getByUsername(request.getUsername());
        if (existingUser != null) {
            log.warn("注册失败，用户名已存在: {}", request.getUsername());
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setIdCard(request.getIdCard());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.WORKER.getCode()); // 默认角色为施工人员
        user.setStatus(1); // 默认状态为启用
        
        save(user);
        log.info("用户注册成功，用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), user.getRole());
        
        return user;
    }
    
    @Override
    public UserProfileResponse getProfile(Long userId) {
        log.info("查询个人信息，用户ID: {}", userId);
        User user = getById(userId);
        if (user == null) {
            throw new com.zzw.zzwgx.common.exception.BusinessException(com.zzw.zzwgx.common.enums.ResultCode.USER_NOT_FOUND);
        }
        
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setIdCard(user.getIdCard());
        response.setPhone(user.getPhone());
        
        log.info("查询个人信息成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        return response;
    }
}


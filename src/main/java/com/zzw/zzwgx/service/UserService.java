package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.entity.User;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {
    
    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);
    
    /**
     * 注册用户
     */
    User register(RegisterRequest request);
    
    /**
     * 获取用户个人信息
     */
    UserProfileResponse getProfile(Long userId);
}


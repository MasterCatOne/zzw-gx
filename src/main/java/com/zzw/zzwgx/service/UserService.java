package com.zzw.zzwgx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzw.zzwgx.dto.request.CreateUserRequest;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.request.UpdateUserRequest;
import com.zzw.zzwgx.dto.response.UserListResponse;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.dto.response.UserViewListResponse;
import com.zzw.zzwgx.entity.User;

import java.util.List;

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
    
    /**
     * 管理员创建用户账号
     */
    User createUser(CreateUserRequest request);
    
    /**
     * 管理员更新用户账号
     */
    User updateUser(Long userId, UpdateUserRequest request);
    
    /**
     * 管理员分页查询用户列表
     */
    Page<UserListResponse> getUserList(Integer pageNum, Integer pageSize, String username, String realName, String roleCode);
    
    /**
     * 获取用户角色代码列表
     */
    List<String> getUserRoleCodes(Long userId);

    /**
     * 获取施工人员列表（仅角色为WORKER），支持按用户名或姓名模糊搜索
     */
    List<UserViewListResponse> listWorkers(Long projectId, String keyword);
}


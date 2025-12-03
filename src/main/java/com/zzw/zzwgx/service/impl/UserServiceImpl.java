package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.enums.UserRole;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.entity.Role;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.entity.UserRoleRelation;
import com.zzw.zzwgx.mapper.RoleMapper;
import com.zzw.zzwgx.mapper.UserMapper;
import com.zzw.zzwgx.mapper.UserRoleRelationMapper;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;
    private final UserRoleRelationMapper userRoleRelationMapper;
    
    @Override
    public User getByUsername(String username) {
        log.debug("根据用户名查询用户，用户名: {}", username);
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
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
        user.setStatus(1); // 默认状态为启用
        
        save(user);
        bindUserRole(user.getId(), UserRole.WORKER.getCode());
        log.info("用户注册成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        
        return user;
    }
    
    @Override
    public UserProfileResponse getProfile(Long userId) {
        log.info("查询个人信息，用户ID: {}", userId);
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setIdCard(user.getIdCard());
        response.setPhone(user.getPhone());
        List<String> roles = getUserRoleCodes(userId);
        response.setRoles(roles);
        if (!roles.isEmpty()) {
            response.setRole(roles.get(0));
        }
        
        log.info("查询个人信息成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        return response;
    }
    
    private void bindUserRole(Long userId, String roleCode) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getRoleCode, roleCode)
                .eq(Role::getDeleted, 0)
                .eq(Role::getRoleStatus, 1));
        if (role == null) {
            throw new BusinessException(ResultCode.USER_ROLE_MISSING);
        }
        UserRoleRelation relation = new UserRoleRelation();
        relation.setUserId(userId);
        relation.setRoleId(role.getId());
        userRoleRelationMapper.insert(relation);
    }
    
    public List<String> getUserRoleCodes(Long userId) {
        List<UserRoleRelation> relations = userRoleRelationMapper.selectList(
                new LambdaQueryWrapper<UserRoleRelation>()
                        .eq(UserRoleRelation::getUserId, userId)
                        .eq(UserRoleRelation::getDeleted, 0));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = relations.stream().map(UserRoleRelation::getRoleId).toList();
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .filter(role -> role.getRoleStatus() != null && role.getRoleStatus() == 1)
                .map(Role::getRoleCode)
                .toList();
    }
}


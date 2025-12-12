package com.zzw.zzwgx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzw.zzwgx.common.enums.ResultCode;
import com.zzw.zzwgx.common.enums.UserRole;
import com.zzw.zzwgx.common.exception.BusinessException;
import com.zzw.zzwgx.dto.request.CreateUserRequest;
import com.zzw.zzwgx.dto.request.RegisterRequest;
import com.zzw.zzwgx.dto.request.UpdateUserRequest;
import com.zzw.zzwgx.dto.request.WorkerUpdateProfileRequest;
import com.zzw.zzwgx.dto.response.UserListResponse;
import com.zzw.zzwgx.dto.response.UserProfileResponse;
import com.zzw.zzwgx.dto.response.UserViewListResponse;
import com.zzw.zzwgx.entity.Cycle;
import com.zzw.zzwgx.entity.Process;
import com.zzw.zzwgx.entity.Role;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.entity.UserRoleRelation;
import com.zzw.zzwgx.mapper.CycleMapper;
import com.zzw.zzwgx.mapper.ProcessMapper;
import com.zzw.zzwgx.mapper.RoleMapper;
import com.zzw.zzwgx.mapper.UserMapper;
import com.zzw.zzwgx.mapper.UserRoleRelationMapper;
import com.zzw.zzwgx.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final CycleMapper cycleMapper;
    private final ProcessMapper processMapper;
    
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
        
        // 检查该用户-角色关联是否已存在（未删除的）
        UserRoleRelation existingRelation = userRoleRelationMapper.selectOne(
                new LambdaQueryWrapper<UserRoleRelation>()
                        .eq(UserRoleRelation::getUserId, userId)
                        .eq(UserRoleRelation::getRoleId, role.getId())
                        .eq(UserRoleRelation::getDeleted, 0)
                        .last("LIMIT 1"));
        
        if (existingRelation != null) {
            // 如果已存在且未删除，则不需要重复插入
            log.debug("用户-角色关联已存在，跳过插入，用户ID: {}, 角色ID: {}", userId, role.getId());
            return;
        }
        
        // 尝试插入新记录，如果失败（唯一约束冲突），则尝试恢复已删除的记录
        try {
            UserRoleRelation relation = new UserRoleRelation();
            relation.setUserId(userId);
            relation.setRoleId(role.getId());
            userRoleRelationMapper.insert(relation);
            log.debug("创建用户-角色关联，用户ID: {}, 角色ID: {}", userId, role.getId());
        } catch (Exception e) {
            // 如果插入失败（可能是唯一约束冲突），尝试恢复已删除的记录
            Throwable cause = e;
            while (cause != null && !(cause instanceof java.sql.SQLIntegrityConstraintViolationException)) {
                cause = cause.getCause();
            }
            
            if (cause instanceof java.sql.SQLIntegrityConstraintViolationException) {
                log.debug("检测到唯一约束冲突，尝试恢复已删除的用户-角色关联，用户ID: {}, 角色ID: {}", userId, role.getId());
                // 使用原生 SQL 直接恢复已删除的记录
                int updated = userRoleRelationMapper.restoreDeletedRelation(userId, role.getId());
                if (updated > 0) {
                    log.debug("恢复用户-角色关联成功，用户ID: {}, 角色ID: {}", userId, role.getId());
                } else {
                    // 如果没有恢复任何记录，说明是其他原因导致的异常，重新抛出
                    log.error("恢复用户-角色关联失败，可能是其他原因，用户ID: {}, 角色ID: {}", userId, role.getId());
                    throw new BusinessException("无法创建用户-角色关联，可能存在数据冲突");
                }
            } else {
                // 其他异常，重新抛出
                throw e;
            }
        }
    }
    
    /**
     * 获取用户角色代码列表（供Service内部和外部调用）
     */
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
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(CreateUserRequest request) {
        log.info("管理员创建用户账号，用户名: {}, 姓名: {}", request.getUsername(), request.getRealName());
        
        // 检查用户名是否已存在
        User existingUser = getByUsername(request.getUsername());
        if (existingUser != null) {
            log.warn("创建用户失败，用户名已存在: {}", request.getUsername());
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setIdCard(request.getIdCard());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1); // 默认启用
        
        save(user);
        
        // 绑定角色
        String roleCode = StringUtils.hasText(request.getRoleCode()) ? request.getRoleCode() : UserRole.WORKER.getCode();
        bindUserRole(user.getId(), roleCode);
        
        log.info("管理员创建用户成功，用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), roleCode);
        return user;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateUser(Long userId, UpdateUserRequest request) {
        log.info("管理员更新用户账号，用户ID: {}", userId);
        
        User user = getById(userId);
        if (user == null) {
            log.warn("更新用户失败，用户不存在，用户ID: {}", userId);
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 更新用户信息
        boolean hasUpdate = false;
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
            hasUpdate = true;
            log.debug("更新用户姓名，用户ID: {}, 新姓名: {}", userId, request.getRealName());
        }
        if (StringUtils.hasText(request.getIdCard())) {
            user.setIdCard(request.getIdCard());
            hasUpdate = true;
            log.debug("更新用户身份证号，用户ID: {}", userId);
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
            hasUpdate = true;
            log.debug("更新用户手机号，用户ID: {}, 新手机号: {}", userId, request.getPhone());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            hasUpdate = true;
            log.debug("更新用户密码，用户ID: {}", userId);
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
            hasUpdate = true;
            log.debug("更新用户状态，用户ID: {}, 新状态: {}", userId, request.getStatus());
        }
        
        // 如果有任何字段需要更新，则执行更新操作
        if (hasUpdate) {
            boolean updateResult = updateById(user);
            if (!updateResult) {
                log.error("更新用户信息失败，用户ID: {}", userId);
                throw new BusinessException("更新用户信息失败");
            }
            log.info("用户信息更新成功，用户ID: {}", userId);
            // 重新获取用户信息，确保返回最新数据
            user = getById(userId);
            if (user == null) {
                log.error("更新后无法获取用户信息，用户ID: {}", userId);
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }
        } else {
            log.debug("没有需要更新的用户信息字段，用户ID: {}", userId);
        }
        
        // 更新角色（如果提供了角色代码）
        if (StringUtils.hasText(request.getRoleCode())) {
            // 获取用户当前角色
            List<String> currentRoles = getUserRoleCodes(userId);
            // 如果新角色与当前角色相同，则不需要更新
            if (currentRoles.contains(request.getRoleCode())) {
                log.debug("用户角色未变化，跳过更新，用户ID: {}, 角色: {}", userId, request.getRoleCode());
            } else {
                // 先删除原有角色关联（逻辑删除）
                userRoleRelationMapper.delete(new LambdaQueryWrapper<UserRoleRelation>()
                        .eq(UserRoleRelation::getUserId, userId)
                        .eq(UserRoleRelation::getDeleted, 0));
                // 绑定新角色（bindUserRole 方法会检查是否已存在，避免重复插入）
                bindUserRole(userId, request.getRoleCode());
                log.debug("更新用户角色，用户ID: {}, 新角色: {}", userId, request.getRoleCode());
            }
        }
        
        log.info("管理员更新用户成功，用户ID: {}", userId);
        return user;
    }
    
    @Override
    public Page<UserListResponse> getUserList(Integer pageNum, Integer pageSize, String username, String realName, String roleCode) {
        log.info("管理员查询用户列表，页码: {}, 每页大小: {}, 用户名: {}, 姓名: {}, 角色: {}", 
                pageNum, pageSize, username, realName, roleCode);
        
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        if (StringUtils.hasText(username)) {
            wrapper.like(User::getUsername, username);
        }
        if (StringUtils.hasText(realName)) {
            wrapper.like(User::getRealName, realName);
        }
        
        wrapper.eq(User::getDeleted, 0);
        wrapper.orderByDesc(User::getCreateTime);
        
        Page<User> userPage = page(page, wrapper);
        
        // 如果有角色过滤条件，需要进一步过滤
        List<User> users = userPage.getRecords();
        if (StringUtils.hasText(roleCode)) {
            users = users.stream()
                    .filter(u -> {
                        List<String> roles = getUserRoleCodes(u.getId());
                        return roles.contains(roleCode);
                    })
                    .collect(Collectors.toList());
        }
        
        // 转换为响应DTO
        Page<UserListResponse> responsePage = new Page<>(pageNum, pageSize, userPage.getTotal());
        List<UserListResponse> responseList = users.stream().map(user -> {
            UserListResponse response = new UserListResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setRealName(user.getRealName());
            response.setIdCard(user.getIdCard());
            response.setPhone(user.getPhone());
            response.setRoles(getUserRoleCodes(user.getId()));
            response.setStatus(user.getStatus());
            response.setCreateTime(user.getCreateTime());
            response.setUpdateTime(user.getUpdateTime());
            return response;
        }).collect(Collectors.toList());
        
        responsePage.setRecords(responseList);
        
        log.info("管理员查询用户列表成功，总数: {}", responsePage.getTotal());
        return responsePage;
    }

    @Override
    public List<UserViewListResponse> listWorkers(Long projectId, String keyword) {
        log.info("查询施工人员列表，项目ID: {}, 关键词: {}", projectId, keyword);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0)
                .eq(User::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        
        // 如果指定项目ID，则仅返回该项目下已有参与记录的施工人员
        Set<Long> projectWorkerIds = null;
        if (projectId != null) {
            List<Cycle> cycles = cycleMapper.selectList(new LambdaQueryWrapper<Cycle>()
                    .eq(Cycle::getProjectId, projectId));
            if (!cycles.isEmpty()) {
                List<Long> cycleIds = cycles.stream().map(Cycle::getId).toList();
                List<Process> processes = processMapper.selectList(new LambdaQueryWrapper<Process>()
                        .in(Process::getCycleId, cycleIds)
                        .isNotNull(Process::getOperatorId));
                projectWorkerIds = processes.stream()
                        .map(Process::getOperatorId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(HashSet::new));
                if (projectWorkerIds.isEmpty()) {
                    // 项目下无已参与的施工人员，直接返回空
                    return Collections.emptyList();
                }
                wrapper.in(User::getId, projectWorkerIds);
            } else {
                // 项目下无循环，返回空
                return Collections.emptyList();
            }
        }
        
        List<User> users = list(wrapper);
        return users.stream()
                .filter(u -> getUserRoleCodes(u.getId()).contains(UserRole.WORKER.getCode()))
                .map(u -> {
                    UserViewListResponse resp = new UserViewListResponse();
                    resp.setId(u.getId());
                    resp.setUsername(u.getUsername());
                    resp.setRealName(u.getRealName());
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileResponse updateWorkerProfile(Long userId, WorkerUpdateProfileRequest request) {
        log.info("施工人员修改个人信息，用户ID: {}", userId);
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getIdCard() != null) {
            user.setIdCard(request.getIdCard());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        updateById(user);
        return getProfile(userId);
    }
}


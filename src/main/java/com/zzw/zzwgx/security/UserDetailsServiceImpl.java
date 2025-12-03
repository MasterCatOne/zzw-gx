package com.zzw.zzwgx.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzw.zzwgx.entity.Role;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.entity.UserRoleRelation;
import com.zzw.zzwgx.mapper.RoleMapper;
import com.zzw.zzwgx.mapper.UserMapper;
import com.zzw.zzwgx.mapper.UserRoleRelationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Spring Security用户详情服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserMapper userMapper;
    private final UserRoleRelationMapper userRoleRelationMapper;
    private final RoleMapper roleMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("加载用户信息，用户名: {}", username);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        
        if (user == null) {
            log.warn("用户不存在，用户名: {}", username);
            throw new UsernameNotFoundException("用户不存在");
        }
        
        if (user.getStatus() == null || user.getStatus() == 0) {
            log.warn("用户已被禁用，用户名: {}", username);
            throw new UsernameNotFoundException("用户已被禁用");
        }
        
        List<String> roleCodes = loadRoleCodes(user.getId());
        log.debug("用户信息加载成功，用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), roleCodes);
        return new SecurityUser(user, roleCodes);
    }
    
    private List<String> loadRoleCodes(Long userId) {
        List<UserRoleRelation> relations = userRoleRelationMapper.selectList(new LambdaQueryWrapper<UserRoleRelation>()
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


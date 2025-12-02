package com.zzw.zzwgx.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzw.zzwgx.entity.User;
import com.zzw.zzwgx.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security用户详情服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserMapper userMapper;
    
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
        
        log.debug("用户信息加载成功，用户ID: {}, 用户名: {}, 角色: {}", user.getId(), user.getUsername(), user.getRole());
        return new SecurityUser(user);
    }
}


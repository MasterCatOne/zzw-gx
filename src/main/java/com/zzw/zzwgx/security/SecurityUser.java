package com.zzw.zzwgx.security;

import com.zzw.zzwgx.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Security用户对象
 */
@Data
public class SecurityUser implements UserDetails {
    
    private final User user;
    private final List<String> roleCodes;
    
    public SecurityUser(User user, List<String> roleCodes) {
        this.user = user;
        this.roleCodes = roleCodes == null ? Collections.emptyList() : roleCodes;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return roleCodes.stream()
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
                .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        return user.getPassword();
    }
    
    @Override
    public String getUsername() {
        return user.getUsername();
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() == 1;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return user.getStatus() == 1;
    }
    
    public Long getUserId() {
        return user.getId();
    }
    
    public List<String> getRoleCodes() {
        return roleCodes;
    }
    
    public String getPrimaryRole() {
        return roleCodes.isEmpty() ? null : roleCodes.get(0);
    }
}


package com.zzw.zzwgx.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * 获取安全的密钥（确保至少64字节用于HS512）
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        
        // HS512需要至少64字节（512位）
        if (keyBytes.length < 64) {
            log.warn("JWT密钥长度不足64字节，当前长度: {}，将使用SHA-256扩展密钥", keyBytes.length);
            try {
                // 使用SHA-256哈希扩展密钥到至少64字节
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(keyBytes);
                
                // 如果哈希后还不够64字节，继续扩展
                if (hash.length < 64) {
                    byte[] extended = new byte[64];
                    System.arraycopy(hash, 0, extended, 0, hash.length);
                    // 重复填充
                    for (int i = hash.length; i < 64; i++) {
                        extended[i] = hash[i % hash.length];
                    }
                    keyBytes = extended;
                } else {
                    keyBytes = hash;
                }
            } catch (Exception e) {
                log.error("扩展JWT密钥失败", e);
                // 如果扩展失败，使用原始密钥并让Keys.hmacShaKeyFor处理
            }
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 生成token
     */
    public String generateToken(Long userId, String username, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        return createToken(claims, username);
    }
    
    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        SecretKey key = getSecretKey();
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 从token中获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            SecretKey key = getSecretKey();
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.debug("解析JWT token失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }
    
    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? ((Number) claims.get("userId")).longValue() : null;
    }
    
    /**
     * 从token中获取角色
     */
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return Collections.emptyList();
        }
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?>) {
            return ((List<?>) rolesObj).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        if (rolesObj instanceof String roleStr) {
            return Collections.singletonList(roleStr);
        }
        return Collections.emptyList();
    }
    
    public String getRoleFromToken(String token) {
        List<String> roles = getRolesFromToken(token);
        return roles.isEmpty() ? null : roles.get(0);
    }
    
    /**
     * 验证token是否有效
     */
    public Boolean validateToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null && !isTokenExpired(claims);
    }
    
    /**
     * 判断token是否过期
     */
    private Boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
}


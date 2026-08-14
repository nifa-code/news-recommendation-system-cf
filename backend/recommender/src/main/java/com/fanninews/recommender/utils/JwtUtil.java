package com.fanninews.recommender.utils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class JwtUtil {
    @Value("${jwt.secret:your-secret-key-at-least-32-chars-long}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认24小时
    private Long expiration;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 从token中提取过期时间
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, String userId) {
        final String username = extractUserId(token);
        return (username.equals(userId) && !isTokenExpired(token));
    }

    public String generateToken(String userId) {
        Map<String,Object>claims=new HashMap<>();
        return createToken(claims,userId);
    }

    private String createToken(Map<String,Object>claims,String userId){
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis()+expiration))
                .signWith(getSigningKey(),SignatureAlgorithm.HS256)
                .compact();
    }

    // ============ 新增功能：黑名单管理（用于密码重置） ============
    /**
     * 将token加入黑名单（用于登出或密码重置时使token失效）
     */
    public void blacklistToken(String token) {
        try {
            // 获取token的剩余有效时间
            Date expiration = extractExpiration(token);
            long currentTime = System.currentTimeMillis();
            long ttl = expiration.getTime() - currentTime;

            if (ttl > 0) {
                // 将token加入Redis黑名单，设置与token相同的过期时间
                String key = "jwt:blacklist:" + token;
                redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);

                // 记录用户最近使用的token（用于密码重置时批量失效）
                String userId = extractUserId(token);
                String userTokenKey = "user:recent:tokens:" + userId;
                redisTemplate.opsForList().leftPush(userTokenKey, token);
                redisTemplate.expire(userTokenKey, 30, TimeUnit.DAYS);

                log.info("Token已加入黑名单: userId={}, ttl={}ms", userId, ttl);
            }
        } catch (Exception e) {
            log.error("将token加入黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = "jwt:blacklist:" + token;
            Boolean hasKey = redisTemplate.hasKey(key);
            return hasKey != null && hasKey;
        } catch (Exception e) {
            log.error("检查token黑名单失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 使指定用户的所有token失效（用于密码重置时）
     */
    public void invalidateAllUserTokens(String userId) {
        try {
            // 方法1: 使用token版本号（推荐）
            // 每次密码重置时递增版本号，验证token时检查版本
            String versionKey = "user:token:version:" + userId;
            Long currentVersion = (Long) redisTemplate.opsForValue().get(versionKey);
            if (currentVersion == null) {
                currentVersion = 1L;
            } else {
                currentVersion++;
            }
            redisTemplate.opsForValue().set(versionKey, currentVersion, 30, TimeUnit.DAYS);

            // 方法2: 批量黑名单用户最近使用的token（可选）
            String userTokenKey = "user:recent:tokens:" + userId;
            Long size = redisTemplate.opsForList().size(userTokenKey);
            if (size != null && size > 0) {
                List<Object> tokens = redisTemplate.opsForList().range(userTokenKey, 0, -1);
                if (tokens != null) {
                    for (Object tokenObj : tokens) {
                        if (tokenObj instanceof String) {
                            blacklistToken((String) tokenObj);
                        }
                    }
                }
            }

            log.info("用户所有token已失效: userId={}, newVersion={}", userId, currentVersion);
        } catch (Exception e) {
            log.error("使用户token失效失败: {}", e.getMessage());
        }
    }

    /**
     * 增强的token验证（检查黑名单和版本号）
     */
    public boolean enhancedValidateToken(String token, String userId) {
        try {
            // 1. 基本验证
            if (!validateToken(token, userId)) {
                return false;
            }

            // 2. 检查token版本号（如果使用了版本机制）
            if (isTokenVersionInvalid(token, userId)) {
                return false;
            }

            // 3. 检查黑名单
            if (isTokenBlacklisted(token)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("增强token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查token版本是否有效
     */
    private boolean isTokenVersionInvalid(String token, String userId) {
        try {
            String versionKey = "user:token:version:" + userId;
            Long currentVersion = (Long) redisTemplate.opsForValue().get(versionKey);

            // 如果没有版本号记录，说明没有强制失效过，token有效
            if (currentVersion == null) {
                return false;
            }

            // 从token中获取版本号（需要在生成token时存入）
            Claims claims = extractAllClaims(token);
            Long tokenVersion = claims.get("version", Long.class);

            // 如果token中没有版本号，或者token版本小于当前版本，则无效
            return tokenVersion == null || tokenVersion < currentVersion;
        } catch (Exception e) {
            log.error("检查token版本失败: {}", e.getMessage());
            return true; // 出错时认为无效
        }
    }

    /**
     * 生成带有版本号的token（用于密码重置后）
     */
    public String generateTokenWithVersion(String userId) {
        Map<String, Object> claims = new HashMap<>();

        // 获取当前版本号
        String versionKey = "user:token:version:" + userId;
        Long currentVersion = (Long) redisTemplate.opsForValue().get(versionKey);
        if (currentVersion == null) {
            currentVersion = 0L;
        }

        // 将版本号存入claims
        claims.put("version", currentVersion);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 刷新token（如果token快过期）
     */
    public String refreshToken(String oldToken) {
        try {
            String userId = extractUserId(oldToken);

            // 验证旧token
            if (!enhancedValidateToken(oldToken, userId)) {
                throw new RuntimeException("无效的token");
            }

            // 将旧token加入黑名单
            blacklistToken(oldToken);

            // 生成新token
            return generateTokenWithVersion(userId);
        } catch (Exception e) {
            log.error("刷新token失败: {}", e.getMessage());
            throw new RuntimeException("刷新token失败");
        }
    }
    /**
     * 从请求头中提取token
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }


    /**
     * 获取token过期时间（秒）
     */
    public Long getExpirationTime() {
        return expiration / 1000; // 转换为秒
    }

    /**
     * 获取token签发时间
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * 检查token是否即将过期（例如30分钟内）
     */
    public boolean isTokenExpiringSoon(String token, long minutes) {
        Date expiration = extractExpiration(token);
        long currentTime = System.currentTimeMillis();
        long timeUntilExpiry = expiration.getTime() - currentTime;
        return timeUntilExpiry < (minutes * 60 * 1000);
    }

    /**
     * 获取token剩余有效时间（毫秒）
     */
    public long getTokenTimeToLive(String token) {
        Date expiration = extractExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }


}

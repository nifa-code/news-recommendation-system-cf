package com.fanninews.recommender.filter;

import com.fanninews.recommender.service.CustomUserDetailsService;
import com.fanninews.recommender.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    // 构造注入
    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    // 重点：保持protected修饰符，正确重写父类方法
    @Override // 必须加@Override注解，确保重写父类方法
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = jwtUtil.extractTokenFromRequest(request);
        if (token != null) {
            try {
                String userId = jwtUtil.extractUserId(token);
                // 增强验证：黑名单+有效期+用户匹配，且未认证过
                if (jwtUtil.enhancedValidateToken(token, userId)
                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // 加载UserDetails，确保principal是UserDetails类型
                    UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                    // 构建正确的认证对象
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,  // 核心：UserDetails而非String
                            null,
                            userDetails.getAuthorities() // 权限列表（无则传空列表）
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 存入SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("JWT认证成功，用户ID：{}", userId);
                }
            } catch (Exception e) {
                log.error("JWT认证失败", e);
                // 认证失败不中断过滤器链，交给Security的异常处理
            }
        }
        filterChain.doFilter(request, response);
    }
}


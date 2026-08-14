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
//@Slf4j
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private JwtUtil jwtUtil;
//
//    private final CustomUserDetailsService userDetailsService;
//
//    // 改为构造注入（因为移除了@Component，无法自动Autowired）
//    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
//        this.jwtUtil = jwtUtil;
//        this.userDetailsService = userDetailsService;
//    }
//
//    @Bean
//    public JwtAuthenticationFilter jwtAuthenticationFilter() {
//        return new JwtAuthenticationFilter(jwtUtil, userDetailsService);
//    }
//    //@Autowired
//    //private CustomUserDetailsService userDetailsService;
//
//    //注释掉的jwt验证存在问题，黑名单验证不可以，并且没有密码重置功能
//
//    @Override
//    public void doFilterInternal(HttpServletRequest request,
//                                 HttpServletResponse response,
//                                 FilterChain filterChain) throws ServletException ,IOException{
//        String token =jwtUtil.extractTokenFromRequest(request);
//        if(token!=null){
//            try{
//                String userId=jwtUtil.extractUserId(token);
//                if(jwtUtil.enhancedValidateToken(token,userId)){
//                    UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(
//                            userId,
//                            null,
//                            Collections.emptyList()
//                    );
//
//                    authentication.setDetails(new HashMap<String,Object>(){{
//                        put("userId",userId);
//                        put("token",token);
//                    }});
//
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                }
//
//            }catch(Exception e){
//                logger.error("无法设置用户认证", e);
//            }
//
//        }
//        filterChain.doFilter(request,response);
//    }


    /*@Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        log.debug("===> JWT过滤器开始执行");
        log.debug("请求URL: {}", request.getRequestURI());
        log.debug("Authorization头: {}", authorizationHeader); // !!!
        String userId=null;
        String jwt=null;
        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
            jwt = authorizationHeader.substring(7);
            userId=jwtUtil.extractUserId(jwt);
            log.debug("从Token中提取的用户ID: {}", userId);
        }
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userId);
                if (jwtUtil.validateToken(jwt, userId)) {
                    log.debug("成功加载用户详情: {}", userDetails.getUsername());
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken
                            (userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }else{
                    log.debug("Token验证失败");
                }
            } catch (Exception e) {
                log.debug("JWT验证失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);

    }*/
//}

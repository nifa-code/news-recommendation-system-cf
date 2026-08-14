package com.fanninews.recommender.controller;
import com.fanninews.recommender.dto.UserProfileDTO;
import com.fanninews.recommender.dto.request.RL.LoginRequest;
import com.fanninews.recommender.dto.request.RL.RegisterRequest;
import com.fanninews.recommender.dto.request.RL.ForgotPasswordRequest;
import com.fanninews.recommender.dto.request.RL.ResetPasswordRequest;
import com.fanninews.recommender.dto.request.UpdateProfileRequest;
import com.fanninews.recommender.dto.response.AuthResponse;
import com.fanninews.recommender.entity.User;
import com.fanninews.recommender.repository.UserRepository;
import com.fanninews.recommender.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户注册、登录、令牌管理")
@Slf4j
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request){
            try{
                AuthResponse result=userService.register(request);
                return ResponseEntity.ok(result);
            }catch(RuntimeException e){
                return ResponseEntity.badRequest().body(e.getMessage());
            }

    }
//    @RateLimiter(
//            key = "auth:login",
//            time = 300,   // 5分钟内
//            count = 10,   // 最多尝试10次
//            limitType = LimitType.IP  // 基于IP限流，防止暴力破解
//    )
    @PostMapping("/login")
    @Operation(summary = "用户登录")
        public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
            try {
                log.info("登录请求 - userId: {}",
                        request.getUserId());
                AuthResponse response = userService.login(request);
                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body("登录失败: " + e.getMessage());
            }
    }


@GetMapping("/me")
@Operation(summary = "获取当前用户信息")
public ResponseEntity<?> getCurrentUser() {
    try {
        //String userId = (String) SecurityContextHolder.getContext()
         //       .getAuthentication().getPrincipal();
        String userId=getCurrentUserId();
        log.info("userid:{}",userId);

        UserProfileDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);

    } catch (RuntimeException e) {
        log.error("获取当前用户信息失败: {}", e.getMessage());
        return ResponseEntity.status(401).body("未授权: " + e.getMessage());
    }
}
    // 添加兼容接口
    @GetMapping("/v1/user/profile")
    @Operation(summary = "获取用户资料（兼容接口）")
    public ResponseEntity<?> getUserProfileCompatible() {
        return getCurrentUser();
    }
    //忘记密码接口
    @PostMapping("/forget-password")
    @Operation(summary="忘记密码（发送重置邮件）")
    public ResponseEntity<?> forgetPassword(@Valid @RequestBody ForgotPasswordRequest request){
        try{
            log.info("忘记密码请求 - email: {}", request.getEmail());
            userService.forgotPassword(request);
            return ResponseEntity.ok("重置密码邮件已发送，请注意查收（有效期2小时）");
        }catch(RuntimeException e){
            log.error("忘记密码失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e) {
            log.error("忘记密码异常: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("服务器内部错误");
        }

    }

    @PutMapping("/v1/user/uprofile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        // 获取当前用户ID
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.updateProfile(userId, request);
        return ResponseEntity.ok("更新成功");
    }  

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码（提交新密码）")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        try{
            log.info("重置密码请求 - token: {}", request.getToken().substring(0, 8) + "****");
            userService.resetPassword(request);
            return ResponseEntity.ok("密码重置成功，已发送确认邮件");
        }catch (RuntimeException e) {
            log.error("重置密码失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("重置密码异常: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("服务器内部错误");
        }
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            throw new AccessDeniedException("认证信息缺失，请重新登录");
        }
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("用户未认证，请先登录");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            if ("anonymousUser".equals(principal)) {
                throw new AccessDeniedException("无效的用户身份");
            }
            return (String) principal;
        } else {
            throw new AccessDeniedException("无法识别的用户身份类型");
        }
    }
}

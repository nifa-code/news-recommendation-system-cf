package com.fanninews.recommender.service;
import com.fanninews.recommender.dto.UserProfileDTO;
import com.fanninews.recommender.dto.request.RL.LoginRequest;
import com.fanninews.recommender.dto.request.UpdateProfileRequest;
import com.fanninews.recommender.dto.response.AuthResponse;
import com.fanninews.recommender.dto.request.RL.ForgotPasswordRequest;
import com.fanninews.recommender.dto.request.RL.RegisterRequest;
import com.fanninews.recommender.dto.request.RL.ResetPasswordRequest;
import com.fanninews.recommender.entity.User;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import com.fanninews.recommender.repository.UserRepository;
import com.fanninews.recommender.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
@Service
@Slf4j
@RequiredArgsConstructor // 关键：Lombok生成包含所有final字段的构造函数，解决注入问题
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JavaMailSender mailSender;
    private final  UserBehaviorRepository userBehaviorRepository;
    private  final NewsRepository newsRepository;


    // ============ 注册逻辑 ============
    public AuthResponse register(RegisterRequest request) {
        // 1. 检查用户ID是否已存在
        if (userRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new RuntimeException("用户ID已存在");
        }

        // 2. 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) { // 修复：原代码用了request.getUserId()，应该是username
            throw new RuntimeException("用户名已存在");
        }

        // 3. 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        User user = User.builder()
                .userId(request.getUserId()) // 匹配UserBuilder的userId方法
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        // 生成JWT token（使用版本号）
        String token = jwtUtil.generateTokenWithVersion(savedUser.getUserId());

        return AuthResponse.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime()) // 需要确保JwtUtil有此方法
                .message("注册成功")
                .build();
    }

    public void updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 检查新用户名是否已存在（如果要改 username）
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }
        // 检查新邮箱是否已存在
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("邮箱已被注册");
            }
            user.setEmail(request.getEmail());
        }
        // 可能还要更新昵称、头像等
        userRepository.save(user);
    }

    // UserService 类中新增方法
    public User findUserById(String userId) {
        // 查询用户，不存在则抛异常
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    // ============ 登录逻辑 ============
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateTokenWithVersion(user.getUserId());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .message("登录成功")
                .build();
    }

    // ============ 忘记密码逻辑 ============
    public void forgotPassword(ForgotPasswordRequest request) {
        // 查找用户
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("该邮箱未注册"));

        checkResetFrequency(user);
        String resetToken = generateResetToken();

        // 保存重置令牌到数据库
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(2));
        user.setPasswordResetAttempts(user.getPasswordResetAttempts() + 1);
        user.setLastPasswordResetRequest(LocalDateTime.now());
        userRepository.save(user);

        // 存入Redis
        String redisKey = "password_reset:token:" + resetToken;
        redisTemplate.opsForValue().set(
                redisKey,
                user.getUserId(),
                2, TimeUnit.HOURS
        );

        try {
            // 使用同步方式发送邮件
            log.info("准备发送密码重置邮件: email={}, username={}",
                    user.getEmail(), user.getUsername());

            // 方法1：直接使用JavaMailSender发送简单邮件测试
            sendSimpleResetEmail(user.getEmail(), user.getUsername(), resetToken);

            // 方法2：如果简单邮件成功，再尝试HTML邮件
            // emailService.sendResetPasswordEmailSync(user.getEmail(), user.getUsername(), resetToken);

            log.info("密码重置邮件发送成功: email={}, username={}",
                    user.getEmail(), user.getUsername());

        } catch (Exception e) {
            log.error("邮件发送失败", e);

            // 邮件发送失败，但业务逻辑已完成（令牌已生成）
            // 可以选择不抛异常，让用户可以使用令牌
            // 或者记录到数据库，稍后重试

            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }
    private void sendSimpleResetEmail(String email, String username, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("2116602182@qq.com");
            message.setTo(email);
            message.setSubject("密码重置请求 - 新闻推荐系统");
            message.setText(String.format(
                    "亲爱的 %s，\n\n" +
                            "请点击以下链接重置密码：\n" +
                            "http://localhost:3000/reset-password?token=%s\n\n" +
                            "该链接有效期为2小时。\n\n" +
                            "如果这不是您本人的操作，请忽略此邮件。",
                    username, token
            ));

            mailSender.send(message);
            log.info("简单重置邮件发送成功: to={}", email);
        } catch (Exception e) {
            log.error("简单重置邮件发送失败", e);
            throw new RuntimeException("邮件发送失败: " + e.getMessage());
        }
    }

    // ============ 重置密码逻辑 ============
    public void resetPassword(ResetPasswordRequest request) {
        // 验证重置令牌
        String userId = verifyResetToken(request.getToken());

        // 验证两次密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("两次输入的密码不一致");
        }

        // 获取用户
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查新旧密码是否相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        // 使该用户的所有JWT token失效
        jwtUtil.invalidateAllUserTokens(userId);

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // 清空重置令牌相关字段
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordResetAttempts(0);
        userRepository.save(user);

        // 清除Redis中的重置令牌
        String redisKey = "password_reset:token:" + request.getToken();
        redisTemplate.delete(redisKey);

        // 发送密码修改成功邮件
        sendPasswordChangedEmail(user.getEmail(), user.getUsername());

        log.info("密码重置成功: userId={}, username={}", userId, user.getUsername());
    }

    // ============ 登出逻辑 ============
    public void logout(String token) {
        jwtUtil.blacklistToken(token);
        log.info("用户已登出，token已加入黑名单");
    }

    // ============ 辅助方法 ============
    private String verifyResetToken(String token) {
        // 先检查Redis缓存
        String redisKey = "password_reset:token:" + token;
        String userId = (String) redisTemplate.opsForValue().get(redisKey);

        if (userId == null) {
            // Redis没有，检查数据库
            User user = userRepository.findByPasswordResetToken(token)
                    .orElseThrow(() -> new RuntimeException("无效的重置令牌"));

            // 检查令牌是否过期
            if (user.getPasswordResetTokenExpiry() == null ||
                    user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("重置令牌已过期");
            }

            return user.getUserId();
        }

        return userId;
    }

    private void checkResetFrequency(User user) {
        if (user.getLastPasswordResetRequest() != null) {
            LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

            if (user.getLastPasswordResetRequest().isAfter(twentyFourHoursAgo) &&
                    user.getPasswordResetAttempts() >= 100) {
                throw new RuntimeException("24小时内重置密码次数已达上限，请稍后再试");
            }
        }
    }

    private String generateResetToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateUserId() {
        return "USER_" + System.currentTimeMillis();
    }

    private void sendPasswordChangedEmail(String toEmail, String username) {
        emailService.sendPasswordChangedEmail(toEmail, username);
    }
    public UserProfileDTO getUserProfile(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        // 从其他服务获取统计数据
        //Integer newsCount = newsRepository.countByUserId(userId);
        Integer likeCount = userBehaviorRepository.countLikesByUserId(userId);
        Integer collectCount = userBehaviorRepository.countCollectsByUserId(userId);

        return new UserProfileDTO(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getLastLogin(),
                likeCount,
                collectCount
        );
    }
}
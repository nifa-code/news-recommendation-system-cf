package com.fanninews.recommender.controller;
import com.fanninews.recommender.dto.request.RL.LoginRequest;
import com.fanninews.recommender.dto.request.RL.RegisterRequest;
import com.fanninews.recommender.service.DeepseekService;
import com.fanninews.recommender.service.EmailService;
import com.fanninews.recommender.utils.JwtUtil;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/debug")
@Slf4j
public class DebugController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private DeepseekService deepseekService;
    @Autowired
    private EmailService emailService;

    @PostMapping("/echo")
    public ResponseEntity<?> echo(@RequestBody Map<String, Object> requestBody) {
        log.info("收到调试请求: {}", requestBody);
        return ResponseEntity.ok(requestBody);
    }

    @PostMapping("/test-login")
    public ResponseEntity<?> testLogin(@RequestBody LoginRequest request) {
        log.info("测试登录 - userId: {}, password: {}",
                request.getUserId(), request.getPassword());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "收到请求");
        response.put("userId", request.getUserId());
        response.put("passwordLength", request.getPassword() != null ? request.getPassword().length() : 0);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-token")
    public String testToken(@RequestBody String token) {
        String userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        boolean isValid = jwtUtil.validateToken(token.replace("Bearer ", ""), userId);
        return "userId: " + userId + ", valid: " + isValid;
    }
    @GetMapping("/deepseek-config")
    public ResponseEntity<?> checkDeepseekConfig() {
        try {
            Field apiKeyField = DeepseekService.class.getDeclaredField("apiKey");
            apiKeyField.setAccessible(true);
            String apiKey = (String) apiKeyField.get(deepseekService);

            return ResponseEntity.ok(Map.of(
                    "configured", apiKey != null && !apiKey.isEmpty(),
                    "key_length", apiKey != null ? apiKey.length() : 0,
                    "key_format_ok", apiKey != null && apiKey.startsWith("sk-")
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/test-deepseek")
    public ResponseEntity<?> testDeepseekConnection() {
        try {
            // 简单的测试请求
            String testUrl = "https://api.deepseek.com/v1/models";
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .build();

            // 从环境变量获取 API Key
            String apiKey = System.getenv("DEEPSEEK_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "环境变量 DEEPSEEK_API_KEY 未设置"
                ));
            }

            Request request = new Request.Builder()
                    .url(testUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                Map<String, Object> result = new HashMap<>();
                result.put("statusCode", response.code());
                result.put("message", response.message());

                if (response.isSuccessful()) {
                    result.put("success", true);
                    result.put("message", "API Key 有效");
                } else {
                    result.put("success", false);
                    if (response.body() != null) {
                        result.put("errorBody", response.body().string());
                    }
                }

                return ResponseEntity.ok(result);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/email")
    public ResponseEntity<String> testEmail() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.qq.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        // 认证器（用你的邮箱+授权码）
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        "2116602182@qq.com",  // 和spring.mail.username一致
                        "raeifkmxxwivecec"     // 刚生成的授权码
                );
            }
        };
        Session session = Session.getInstance(props, auth);
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress("2788850080@qq.com"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress("2788850080@qq.com"));
            message.setSubject("原生测试邮件");
            message.setText("这是原生JavaMail测试邮件");

            Transport.send(message);
            return ResponseEntity.ok("原生测试邮件已发送");
        } catch (Exception e) {
            log.error("原生邮件发送失败", e);
            return ResponseEntity.badRequest().body("原生测试失败：" + e.getMessage());
        }
    }



}
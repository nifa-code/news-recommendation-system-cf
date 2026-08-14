package com.fanninews.recommender.dto.response;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Builder  // ← 添加这个注解
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private String userId;
    private String email;
    private String username;
    private Long expiresIn;
    private String message;
}
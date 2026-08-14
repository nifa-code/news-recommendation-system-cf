package com.fanninews.recommender.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Column(nullable = false)
    @JsonIgnore // 重要：序列化时忽略密码
    private String password;

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(unique = true, length = 100)
    private String username;

    @Column(name = "nickname")
    private String nickname;  // 昵称（前端展示用）

    @Column(name = "avatar_url")
    private String avatarUrl;  // 头像URL

    @Column(name = "email")
    private String email;      // 邮箱

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // ===== 找回密码相关字段 =====
    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Column(name = "password_reset_attempts", columnDefinition = "int default 0")
    private Integer passwordResetAttempts = 0;

    @Column(name = "last_password_reset_request")
    private LocalDateTime lastPasswordResetRequest;

    public static class UserBuilder {
        private Integer passwordResetAttempts = 0;

        // 可选：防止手动设置 null
        public UserBuilder passwordResetAttempts(Integer passwordResetAttempts) {
            this.passwordResetAttempts = passwordResetAttempts == null ? 0 : passwordResetAttempts;
            return this;
        }
    }
}
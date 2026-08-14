package com.fanninews.recommender.repository;

import com.fanninews.recommender.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 按 userId 查找
    Optional<User> findByUserId(String userId);

    // 按 username 查找
    Optional<User> findByUsername(String username);

    // 按 email 查找
    Optional<User> findByEmail(String email);

    // 按密码重置令牌查找
    Optional<User> findByPasswordResetToken(String token);

    // 检查是否存在
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);  // ← 添加这个方法
}

package com.fanninews.recommender.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now(),
                "service", "新闻推荐系统后端"
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        return ResponseEntity.ok(Map.of(
                "message", "前后端连接测试成功",
                "time", System.currentTimeMillis()
        ));
    }
}
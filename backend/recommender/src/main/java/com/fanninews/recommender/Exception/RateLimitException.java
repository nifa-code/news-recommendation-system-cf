package com.fanninews.recommender.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

public class RateLimitException extends RuntimeException {

    // 可选的：等待时间（秒），用于告诉客户端多久后可以重试
    private Long waitTimeSeconds;

    /**
     * 基础构造方法
     * @param message 异常信息
     */
    public RateLimitException(String message) {
        super(message);
    }

    /**
     * 携带等待时间的构造方法
     * @param message 异常信息
     * @param waitTimeSeconds 建议等待时间（秒）
     */
    public RateLimitException(String message, Long waitTimeSeconds) {
        super(message);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    /**
     * 携带原因和等待时间的构造方法
     * @param message 异常信息
     * @param cause 原始异常
     * @param waitTimeSeconds 建议等待时间（秒）
     */
    public RateLimitException(String message, Throwable cause, Long waitTimeSeconds) {
        super(message, cause);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    // Getter 方法
    public Long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    /**
     * 格式化错误信息（包含等待时间）
     */
    @Override
    public String getMessage() {
        if (waitTimeSeconds != null && waitTimeSeconds > 0) {
            return super.getMessage() + "，请 " + waitTimeSeconds + " 秒后重试";
        }
        return super.getMessage();
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(RateLimitException ex) {
        return ResponseEntity.status(429).body(Map.of("error", ex.getMessage()));
    }
}

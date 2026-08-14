package com.fanninews.recommender.Exception;

import com.fanninews.recommender.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<?>> handleRateLimitException(
            RateLimitException e, HttpServletRequest request) {

        log.warn("接口限流触发 - URI: {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS) // 429
                .body(ApiResponse.error("TOO_MANY_REQUESTS", e.getMessage()));
    }

    /**
     * 处理业务异常（RuntimeException）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(
            RuntimeException e, HttpServletRequest request) {

        log.error("业务异常 - URI: {}, 错误: {}",
                request.getRequestURI(), e.getMessage(), e);

        // 根据异常信息判断返回什么错误码
        String code = "BAD_REQUEST";
        String message = e.getMessage();

        if (message.contains("已存在") || message.contains("exists")) {
            code = "RESOURCE_EXISTS";
        } else if (message.contains("密码") || message.contains("凭证")) {
            code = "INVALID_CREDENTIALS";
            message = "用户名或密码错误"; // 模糊提示，更安全
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(code, message));
    }

    /**
     * 处理参数验证异常（@Valid 触发的）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.error("参数验证失败: {}", ex.getMessage());

        // 收集所有验证错误
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", "VALIDATION_ERROR");
        response.put("message", "参数验证失败");
        response.put("errors", errors);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.badRequest().body(response);
    }

    // 处理JSON解析异常
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonParseException(
            HttpMessageNotReadableException ex) {

        log.error("JSON解析失败: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", "JSON_PARSE_ERROR");
        response.put("message", "请求体JSON格式错误");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.badRequest().body(response);
    }
}
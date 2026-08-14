package com.fanninews.recommender.dto.response;

import lombok.Data;

import java.io.Serializable;

@Data
public class ApiResponse<T> implements Serializable {
    private boolean success;
    private String code;
    private String message;
    private T data;
    private Long timestamp;

    // 静态工厂方法
    public static <T> ApiResponse<T> success(T data) {
        return success("SUCCESS", "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error("ERROR", message);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    // 链式调用方法（可选）
    public ApiResponse<T> setData(T data) {
        this.data = data;
        return this;
    }

    public ApiResponse<T> setMessage(String message) {
        this.message = message;
        return this;
    }


}

package com.fanninews.recommender.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DeepSeekRequest {
    private String model;
    private List<Message> messages;
    private boolean stream = false; // 默认为非流式
    private Double temperature = 0.7;
    private Integer max_tokens = 500;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
    public DeepSeekRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }
}


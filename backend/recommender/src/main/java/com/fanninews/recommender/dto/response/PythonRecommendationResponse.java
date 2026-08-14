package com.fanninews.recommender.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class PythonRecommendationResponse {
    private Boolean success;
    private String userId;
    private List<PythonRecommendationItem> recommendations;
    private String generatedAt;
    private Object modelInfo; // 可以更具体定义

    @Data
    public static class PythonRecommendationItem {
        private String newsId;
        private Double score;
        private Integer rank;
    }
}

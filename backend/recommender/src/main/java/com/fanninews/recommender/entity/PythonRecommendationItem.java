package com.fanninews.recommender.entity;

import lombok.Data;

@Data
public class PythonRecommendationItem {
    private String newsId;
    private Double score;
    private String title;
    private String category;
}

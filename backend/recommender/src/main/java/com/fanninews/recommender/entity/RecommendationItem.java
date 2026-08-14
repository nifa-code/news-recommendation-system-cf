package com.fanninews.recommender.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecommendationItem {
    @NotBlank(message = "新闻ID不能为空")
    private String newsId;

    @Min(value = 0, message = "分数不能小于0")
    @Max(value = 10, message = "分数不能大于10")
    private double score;

    @Min(value = 0, message = "置信度不能小于0")
    @Max(value = 1, message = "置信度不能大于1")
    private double confidence = 0.5;

    // 可选字段
    private Integer rank;
    private String reason;
}
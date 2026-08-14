package com.fanninews.recommender.dto.response;

import lombok.Data;

@Data
public class RecommendationDTO {
    private String newsId;
    private double score;
    private int rank;
    private String title;        // 新闻标题
    private String summary;      // 新闻摘要
    private String imageUrl;     // 新闻图片
    private String category;     // 新闻类别

    // 构造方法
    public RecommendationDTO() {}

    public RecommendationDTO(String newsId, double score, int rank) {
        this.newsId = newsId;
        this.score = score;
        this.rank = rank;
    }
}
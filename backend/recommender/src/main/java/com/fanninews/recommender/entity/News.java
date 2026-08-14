package com.fanninews.recommender.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "news")
@Data
public class News {
    @Id
    @Column(name="news_id")
    private String newsId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(length = 100)
    private String category;

    private String subcategory;

    @Column(length = 500)
    private String url;

    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    // =========== 新增图片相关字段 ===========
    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;  // 封面图URL

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;   // 缩略图URL（可选）

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;      // 多张图片URL（JSON数组）

    @Column(name = "has_images")
    private Boolean hasImages = false;  // 是否包含图片

    @Column(name = "image_count")
    private Integer imageCount = 0;     // 图片数量
    // ======================================

    // 新增字段：浏览量
    @Column(name = "view_count")
    private Integer viewCount = 0;

    // 新增字段：点赞数（可选）
    @Column(name = "like_count")
    private Integer likeCount = 0;

    // 新增字段：收藏数（可选）
    @Column(name = "collect_count")
    private Integer collectCount = 0;

    // 新增字段：是否热门标记
    @Column(name = "is_hot")
    private Boolean isHot = false;

    // 新增字段：热度分数（综合计算）
    @Column(name = "heat_score")
    private Double heatScore = 0.0;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        // 初始化默认值
        if (viewCount == null) viewCount = 0;
        if (likeCount == null) likeCount = 0;
        if (collectCount == null) collectCount = 0;
        if (isHot == null) isHot = false;
        if (heatScore == null) heatScore = 0.0;
        if (imageCount == null) imageCount = 0;
        if (hasImages == null) hasImages = false;

        // 自动判断是否有图片
        if (coverImageUrl != null && !coverImageUrl.trim().isEmpty()) {
            this.hasImages = true;
            this.imageCount = Math.max(this.imageCount, 1);
        }
    }
}

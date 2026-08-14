package com.fanninews.recommender.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class NewsDTO {
    private String id;
    private String title;
    private String category;
    private String abstractText;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;
    private Integer viewCount;
    private Double recommendationScore;
    // =========== 新增图片相关字段 ===========
    private String coverImageUrl;    // 封面图URL
    private String thumbnailUrl;     // 缩略图URL
    private List<String> imageUrls;  // 多张图片URL列表
    private Boolean hasImages;       // 是否包含图片
    private Integer imageCount;      // 图片数量
    // ======================================

    // 获取显示图片（优先级：缩略图 > 封面图 > 第一张图片）
    @JsonIgnore
    public String getDisplayImage() {
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            return thumbnailUrl;
        }
        if (coverImageUrl != null && !coverImageUrl.isEmpty()) {
            return coverImageUrl;
        }
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return "https://picsum.photos/400/200?random=" + id;
    }

    // 转换imageUrls字符串为List
    @JsonProperty("imageUrls")
    public List<String> getImageUrls() {
        if (imageUrls != null) {
            return imageUrls;
        }
        // 如果数据库中存储的是JSON字符串，可以在这里解析
        return new ArrayList<>();
    }
}

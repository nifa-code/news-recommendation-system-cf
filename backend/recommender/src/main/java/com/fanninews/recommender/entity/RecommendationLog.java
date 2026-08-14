package com.fanninews.recommender.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendation_log")
@Data
public class RecommendationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "recommended_news_ids", columnDefinition = "TEXT")
    private String recommendedNewsIds; // 可以存储JSON字符串，如 ["N123", "N456"]

    @Column(length = 50)
    private String algorithm = "UserCF";

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;
}

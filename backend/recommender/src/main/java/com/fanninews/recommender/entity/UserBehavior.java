package com.fanninews.recommender.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_behavior")
@Data
public class UserBehavior {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自增主键
    private Long id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "news_id", nullable = false)
    private String newsId;

    @Enumerated(EnumType.STRING) // 枚举类型存储为字符串
    @Column(name = "behavior_type")
    private BehaviorType behaviorType;

    @Column(name = "behavior_time")
    private LocalDateTime behaviorTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    // 定义行为类型枚举
    public enum BehaviorType {
        VIEW, CLICK, LIKE, COLLECT
    }
}

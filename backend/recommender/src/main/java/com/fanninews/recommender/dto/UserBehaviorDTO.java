package com.fanninews.recommender.dto;

import com.fanninews.recommender.entity.UserBehavior;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserBehaviorDTO {
    private Long id;
    private String userId;
    private String newsId;
    private UserBehavior.BehaviorType behaviorType;
    private LocalDateTime behaviorTime;
    private Integer durationSeconds;
}
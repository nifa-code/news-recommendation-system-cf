package com.fanninews.recommender.dto.request;

import com.fanninews.recommender.entity.UserBehavior;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordBehaviorRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String newsId;

    @NotNull
    private UserBehavior.BehaviorType behaviorType;

    private Integer durationSeconds;
}
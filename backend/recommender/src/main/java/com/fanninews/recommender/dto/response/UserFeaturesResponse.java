package com.fanninews.recommender.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class UserFeaturesResponse {
    private String userId;
    private Map<String, Long> categoryPreferences;
    private Map<String, Long> timePreferences;
    private int totalInteractions;
    private LocalDateTime lastActive;
}

package com.fanninews.recommender.dto.response;

import com.fanninews.recommender.dto.UserBehaviorDTO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BehaviorDataResponse {
    private List<UserBehaviorDTO> behaviors;
    private long userCount;
    private long newsCount;
    private long totalRecords;
    private LocalDateTime generatedAt;
}
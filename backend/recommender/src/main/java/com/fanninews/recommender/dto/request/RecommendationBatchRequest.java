package com.fanninews.recommender.dto.request;

import com.fanninews.recommender.entity.RecommendationItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendationBatchRequest {
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotNull(message = "推荐列表不能为空")
    @Size(min = 1, max = 100, message = "推荐列表长度必须在1-100之间")
    private List<RecommendationItem> recommendations;

    private String algorithm = "user_cf";

    @NotNull(message = "生成时间不能为空")
    private LocalDateTime generatedAt;
}
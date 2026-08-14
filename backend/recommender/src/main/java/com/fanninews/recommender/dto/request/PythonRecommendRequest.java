package com.fanninews.recommender.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * 匹配Python FastAPI的RecommendRequest模型
 */
@Data
public class PythonRecommendRequest {
    @NotBlank(message = "用户ID不能为空")
    @JsonProperty("user_id")  // 确保JSON字段名和Python端一致
    private String userId;

    @Min(1)
    @Max(100)
    @JsonProperty("top_k")     // 匹配Python的top_k参数
    private int topK = 10;

    @JsonProperty("use_diversity")
    private boolean useDiversity = true;
}

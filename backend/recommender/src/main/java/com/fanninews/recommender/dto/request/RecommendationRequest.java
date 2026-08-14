package com.fanninews.recommender.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;



@Data
public class RecommendationRequest {
    @NotNull(message = "用户ID不能为空")
    private String userId;

    @Min(value=1,message="推荐数至少为1")
    @Max(value=50,message="推荐数最多为50")
    private Integer count=10;
    //分类过滤
    private String category;
    private boolean useDiversity=true;

    public Object getUseDiversity() {
        return useDiversity;
    }
}

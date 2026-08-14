package com.fanninews.recommender.dto.request;

import com.fanninews.recommender.entity.UserBehavior;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank(message = "新闻ID不能为空")
    private String newsId;

    @NotBlank(message = "行为类型不能为空")
    private String behaviorType; // VIEW, CLICK, LIKE, COLLECT

    private Integer durationSeconds; // 停留时长（秒）

    // 将FeedbackRequest转换为UserBehavior的便捷方法
    public UserBehavior toUserBehavior(String userId) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setNewsId(this.newsId);
        behavior.setBehaviorType(UserBehavior.BehaviorType.valueOf(this.behaviorType));
        behavior.setDurationSeconds(this.durationSeconds);
        return behavior;
    }
}

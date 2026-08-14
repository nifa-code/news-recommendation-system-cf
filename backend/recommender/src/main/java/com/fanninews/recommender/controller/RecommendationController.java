package com.fanninews.recommender.controller;
import com.fanninews.recommender.dto.request.FeedbackRequest;
import com.fanninews.recommender.dto.response.ApiResponse;
import com.fanninews.recommender.dto.response.RecommendationDTO;
import com.fanninews.recommender.entity.LimitType;
import com.fanninews.recommender.service.UserBehaviorService;
import com.fanninews.recommender.utils.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import com.fanninews.recommender.dto.request.RecommendationRequest;
import com.fanninews.recommender.dto.response.RecommendationResponse;
import com.fanninews.recommender.service.RecommendationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recommend")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private UserBehaviorService userBehaviorService;

    @GetMapping
    @Operation(summary = "获取个性化新闻推荐")
    public ResponseEntity<RecommendationResponse> recommend(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int count) {
        String currentUserId = getCurrentUserId();
        RecommendationRequest req = new RecommendationRequest();
        req.setUserId(currentUserId);
        req.setCount(count);
        RecommendationResponse resp = recommendationService.getRecommendations(req);
        return ResponseEntity.ok(resp);
    }
    @GetMapping("/hot")
    @RateLimiter(
            key = "recommend:hot",
            time = 60,      // 每分钟
            count = 100,    // 热门接口允许更多请求
            limitType = LimitType.IP  // 基于IP限流
    )
    @Operation(summary = "获取热 门新闻推荐")
    public ResponseEntity<RecommendationResponse> hotRecommendations(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int count) {
        RecommendationRequest req = new RecommendationRequest();
        req.setUserId("anonymous");
        req.setCount(count);
        String reason="Get Hot News!";
        RecommendationResponse resp = recommendationService.getHotNewsResponse(req.getUserId(),req.getCount(),reason);
        return ResponseEntity.ok(resp);
    }
    @PostMapping("/refresh")
    @RateLimiter(
            key = "recommend:refresh",
            time = 300,    // 5分钟内
            count = 5,     // 只能刷新5次
            limitType = LimitType.USER
    )
    @Operation(summary = "刷新推荐列表")
    public ResponseEntity<RecommendationResponse> refreshRecommendations() {
        String currentUserId = getCurrentUserId();
        RecommendationRequest req=new RecommendationRequest();
        req.setUserId(currentUserId);
        req.setCount(20);
        // 调用服务刷新缓存或重新计算
        RecommendationResponse resp = recommendationService.getRecommendations(req);
        return ResponseEntity.ok(resp);
    }
    /**
     * 根据行为类型获取权重
     */
    private double getBehaviorWeight(String behaviorType) {
        return switch (behaviorType.toUpperCase()) {
            case "LIKE" -> 0.8;
            case "COLLECT" -> 1.0;
            case "SHARE" -> 0.9;
            case "VIEW" -> 0.3;
            default -> 0.5;
        };
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. 确保认证信息存在
        if (authentication == null) {
            throw new AccessDeniedException("认证信息缺失，请重新登录");
        }

        // 2. 确保用户已认证（不是匿名用户）
        if (!authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("用户未认证，请先登录");
        }

        // 3. 安全地提取用户名
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            // 确保这个String不是匿名标识
            if ("anonymousUser".equals(principal)) {
                throw new AccessDeniedException("无效的用户身份");
            }
            return (String) principal;
        } else {
            throw new AccessDeniedException("无法识别的用户身份类型");
        }
    }
}
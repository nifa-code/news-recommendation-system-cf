package com.fanninews.recommender.controller;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.fanninews.recommender.dto.request.RecordBehaviorRequest;
import com.fanninews.recommender.dto.response.ApiResponse;
import com.fanninews.recommender.entity.UserBehavior;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import com.fanninews.recommender.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api/v1/news/{newsId}/behavior")
@Slf4j
public class BehaviorController {

    private final UserBehaviorService userBehaviorService;
    private final UserBehaviorRepository userBehaviorRepository;

    @Autowired
    public BehaviorController(UserBehaviorService userBehaviorService, UserBehaviorRepository userBehaviorRepository) {
        this.userBehaviorService = userBehaviorService;
        this.userBehaviorRepository = userBehaviorRepository;
    }

    @PostMapping("/like")
    @Operation(summary = "点赞/取消点赞新闻")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("用户未登录"));
            }
            String finalUserId = userDetails.getUsername();

            boolean isLike = userBehaviorService.toggleLike(finalUserId, newsId);

            Map<String, Object> result = new HashMap<>();
            result.put("liked", isLike);
            result.put("likeCount", userBehaviorService.getNewsLikeCount(newsId));
            result.put("message", isLike ? "点赞成功" : "取消点赞成功");

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("点赞操作失败", e);
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }


    @PostMapping("/collect")
    @Operation(summary = "收藏/取消收藏新闻")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleCollect(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }

            String finalUserId = userDetails.getUsername();

            if (finalUserId == null || finalUserId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("用户ID无效"));
            }

            System.out.println("收藏操作，用户ID: " + finalUserId + ", 新闻ID: " + newsId);

            boolean isCollect = userBehaviorService.toggleCollect(finalUserId, newsId);

            Map<String, Object> result = new HashMap<>();
            result.put("collected", isCollect);
            result.put("collectCount", userBehaviorService.getNewsCollectCount(newsId));
            result.put("message", isCollect ? "收藏成功" : "取消收藏成功");

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("收藏操作失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("收藏操作失败: " + e.getMessage()));
        }
    }





    @GetMapping("/status")
    @Operation(summary = "获取用户对新闻的行为状态")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getUserBehaviorStatus(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 允许匿名访问，但如果没有登录用户，只返回新闻状态，不包含用户行为状态
            Map<String, Boolean> status = new HashMap<>();

            // 如果有登录用户，获取用户行为状态
            if (userDetails != null) {
                String finalUserId = userDetails.getUsername();

                if (finalUserId != null && !finalUserId.trim().isEmpty()) {
                    status.put("liked", userBehaviorService.checkUserLiked(finalUserId, newsId));
                    status.put("collected", userBehaviorService.checkUserCollected(finalUserId, newsId));
                    status.put("read", userBehaviorService.checkUserRead(finalUserId, newsId));
                }
            }

            // 总是包含新闻状态（可以为false）
            status.putIfAbsent("liked", false);
            status.putIfAbsent("collected", false);
            status.putIfAbsent("read", false);

            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("获取行为状态失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取行为状态失败: " + e.getMessage()));
        }
    }




    @PostMapping("/read")
    public ResponseEntity<ApiResponse<String>> recordRead(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }

            String finalUserId = userDetails.getUsername();

            if (finalUserId == null || finalUserId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("用户ID无效"));
            }

            System.out.println("记录阅读行为，用户ID: " + finalUserId + ", 新闻ID: " + newsId);

            userBehaviorService.recordRead(finalUserId, newsId);
            return ResponseEntity.ok(ApiResponse.success("阅读记录成功"));
        } catch (Exception e) {
            log.error("阅读记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("阅读记录失败: " + e.getMessage()));
        }
    }

    @PostMapping("/click")
    public ResponseEntity<ApiResponse<String>> recordClick(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }

            String finalUserId = userDetails.getUsername();

            if (finalUserId == null || finalUserId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("用户ID无效"));
            }

            System.out.println("记录点击行为，用户ID: " + finalUserId + ", 新闻ID: " + newsId);

            userBehaviorService.recordClick(finalUserId, newsId);
            return ResponseEntity.ok(ApiResponse.success("点击记录成功"));
        } catch (Exception e) {
            log.error("点击记录失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("点击记录失败: " + e.getMessage()));
        }
    }
}

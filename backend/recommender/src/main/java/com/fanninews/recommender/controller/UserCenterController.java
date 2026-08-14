package com.fanninews.recommender.controller;
import com.fanninews.recommender.dto.response.ApiResponse;
import com.fanninews.recommender.entity.UserBehavior;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import com.fanninews.recommender.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人中心控制器
 * 提供浏览历史、收藏列表等接口
 */
@RestController
@RequestMapping("/api/v1/user-center")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "用户个人中心", description = "浏览历史、收藏列表等接口")
public class UserCenterController {
    private final UserBehaviorService userBehaviorService;
    private final UserBehaviorRepository userBehaviorRepository;
    /**
     * 获取用户浏览历史（带分页）
     */
    @GetMapping("/view-history")
    @Operation(summary = "获取用户浏览历史", description = "返回用户浏览过的新闻列表，按浏览时间倒序")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getViewHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "页码，默认1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页条数，默认10") @RequestParam(defaultValue = "10") int pageSize) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            String userId = userDetails.getUsername();
            List<Map<String, Object>> historyList = userBehaviorService.getUserViewHistory(userId, pageNum, pageSize);
            long total = userBehaviorService.getUserViewHistoryCount(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("list", historyList);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取浏览历史失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取浏览历史失败: " + e.getMessage()));
        }
    }


    @GetMapping("/like-list")
    @Operation(summary = "获取用户点赞列表", description = "返回用户点赞的新闻列表，按点赞时间倒序")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLikeList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            String userId = userDetails.getUsername();

            List<Map<String, Object>> likeList = userBehaviorService.getUserLikeList(userId, pageNum, pageSize);
            long total = userBehaviorService.getUserLikeListCount(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("list", likeList);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取点赞列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取点赞列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户收藏列表（带分页）
     */
    @GetMapping("/collect-list")
    @Operation(summary = "获取用户收藏列表", description = "返回用户收藏的新闻列表，按收藏时间倒序")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCollectList(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            String userId = userDetails.getUsername();
            List<Map<String, Object>> collectList = userBehaviorService.getUserCollectList(userId, pageNum, pageSize);
            long total = userBehaviorService.getUserCollectListCount(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("list", collectList);
            result.put("total", total);
            result.put("pageNum", pageNum);
            result.put("pageSize", pageSize);
            result.put("totalPages", (total + pageSize - 1) / pageSize);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取收藏列表失败: " + e.getMessage()));
        }
    }

    /**
     * 清空用户浏览历史
     */
    @DeleteMapping("/delete-history")
    @Operation(summary = "清空浏览历史", description = "删除用户所有的浏览记录")
    public ResponseEntity<ApiResponse<String>> clearViewHistory(
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            String userId = userDetails.getUsername();

            // 删除用户所有VIEW行为
            List<UserBehavior> viewBehaviors = userBehaviorRepository.findByUserIdAndBehaviorTypeOrderByBehaviorTimeDesc(
                    userId, UserBehavior.BehaviorType.VIEW);
            userBehaviorRepository.deleteAll(viewBehaviors);

            return ResponseEntity.ok(ApiResponse.success("浏览历史清空成功"));
        } catch (Exception e) {
            log.error("清空浏览历史失败", e);
            return ResponseEntity.ok(ApiResponse.error("清空浏览历史失败: " + e.getMessage()));
        }
    }

    /**
     * 取消单条收藏
     */
    @DeleteMapping("/collect/{newsId}")
    @Operation(summary = "取消收藏", description = "取消用户对指定新闻的收藏")
    public ResponseEntity<ApiResponse<String>> cancelCollect(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            String userId = userDetails.getUsername();
            boolean isCanceled = !userBehaviorService.toggleCollect(userId, newsId);
            if (isCanceled) {
                return ResponseEntity.ok(ApiResponse.success("取消收藏成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("取消收藏失败，该新闻未被收藏"));
            }
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return ResponseEntity.ok(ApiResponse.error("取消收藏失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/like/{newsId}")
    @Operation(summary = "取消点赞", description = "取消用户对指定新闻的点赞")
    public ResponseEntity<ApiResponse<String>> cancelLike(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("用户未登录"));
            }
            String userId = userDetails.getUsername();
            boolean isCanceled = !userBehaviorService.toggleLike(userId, newsId);
            if (isCanceled) {
                return ResponseEntity.ok(ApiResponse.success("取消点赞成功"));
            } else {
                return ResponseEntity.ok(ApiResponse.error("取消点赞失败，该新闻未被点赞"));
            }
        } catch (Exception e) {
            log.error("取消点赞失败", e);
            return ResponseEntity.ok(ApiResponse.error("取消点赞失败: " + e.getMessage()));
        }
    }
}

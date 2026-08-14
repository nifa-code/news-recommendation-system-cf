package com.fanninews.recommender.controller;
import com.fanninews.recommender.dto.UserBehaviorDTO;
import com.fanninews.recommender.dto.request.BatchNewsRequest;
import com.fanninews.recommender.dto.request.RecommendationBatchRequest;
import com.fanninews.recommender.dto.response.ApiResponse;
import com.fanninews.recommender.dto.response.BehaviorDataResponse;
import com.fanninews.recommender.dto.response.UserFeaturesResponse;
import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.entity.UserBehavior;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import com.fanninews.recommender.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ml")
@Slf4j
public class MLDataController {
    @Autowired
    private UserBehaviorService userBehaviorService;
    @Autowired
    private UserBehaviorRepository userBehaviorRepository;
    @Autowired
    private NewsRepository newsRepository;
    /**
     * 为Python协同过滤算法提供用户行为数据
     */
    @GetMapping("/behavior-data")
    @Operation(summary = "获取用户行为数据（用于协同过滤训练）")
    public ResponseEntity<BehaviorDataResponse> getBehaviorDataForTraining(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue="10000")int limit) {

        LocalDateTime sinceTime = LocalDateTime.now().minusDays(days);

        // 获取用户行为数据
        Page<UserBehavior>behaviorPage=userBehaviorRepository.findByBehaviorTimeAfter(sinceTime, PageRequest.of(0, limit));
        //List<UserBehavior> behaviors = userBehaviorRepository.findByBehaviorTimeAfter(sinceTime);
        List<UserBehavior>behaviors=behaviorPage.getContent();

        // 转换为算法需要的格式
        List<UserBehaviorDTO> behaviorDTOs = behaviors.stream()
                .map(b -> {
                    UserBehaviorDTO dto = new UserBehaviorDTO();
                    dto.setUserId(b.getUserId());
                    dto.setNewsId(b.getNewsId());
                    dto.setBehaviorType(b.getBehaviorType());
                    dto.setBehaviorTime(b.getBehaviorTime());
                    return dto;
                })
                .collect(Collectors.toList());

        // 统计数据信息
        long userCount = behaviors.stream().map(UserBehavior::getUserId).distinct().count();
        long newsCount = behaviors.stream().map(UserBehavior::getNewsId).distinct().count();
        long totalRecords = behaviors.size();

        BehaviorDataResponse response = new BehaviorDataResponse();
        response.setBehaviors(behaviorDTOs);
        response.setUserCount(userCount);
        response.setNewsCount(newsCount);
        response.setTotalRecords(totalRecords);
        response.setGeneratedAt(LocalDateTime.now());

        log.info("为Python算法提供数据: {}条记录，{}用户，{}新闻",
                totalRecords, userCount, newsCount);

        return ResponseEntity.ok(response);
    }
    /**
     * 获取用户特征数据（用于推荐算法）
     */
    @GetMapping("/user-features/{userId}")
    public ResponseEntity<ApiResponse<UserFeaturesResponse>> getUserFeatures(
            @PathVariable String userId,
            @RequestParam(defaultValue = "30") int days) {
        try {
            LocalDateTime sinceTime = LocalDateTime.now().minusDays(days);

            // 统计用户偏好
            Map<String, Long> categoryPreferences = calculateCategoryPreferences(userId, sinceTime);
            Map<String, Long> timePreferences = calculateTimePreferences(userId, sinceTime);

            // 获取用户行为历史
            List<UserBehavior> userBehaviors = userBehaviorRepository
                    .findRecentBehaviorsByUserId(userId, PageRequest.of(0, 1000));

            UserFeaturesResponse response = new UserFeaturesResponse();
            response.setUserId(userId);
            response.setCategoryPreferences(categoryPreferences);
            response.setTimePreferences(timePreferences);
            response.setTotalInteractions(userBehaviors.size());
            response.setLastActive(getLastActiveTime(userId));

            log.info("获取用户 {} 的特征数据，类别偏好: {} 个", userId, categoryPreferences.size());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("获取用户特征失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取用户特征失败: " + e.getMessage()));
        }
    }

    /**
     * 实现：根据用户历史计算分类偏好
     */
    private Map<String, Long> calculateCategoryPreferences(String userId, LocalDateTime sinceTime) {
        Map<String, Long> categoryPreferences = new HashMap<>();

        try {
            // 方法1：直接查询统计（更高效）
            List<Object[]> results = userBehaviorRepository.countBehaviorByCategory(userId, sinceTime);

            for (Object[] result : results) {
                String category = (String) result[0];
                Long count = (Long) result[1];
                categoryPreferences.put(category, count);
            }

            // 如果直接查询没有结果，使用备用方法
            if (categoryPreferences.isEmpty()) {
                categoryPreferences = calculateCategoryPreferencesAlternative(userId, sinceTime);
            }

        } catch (Exception e) {
            log.warn("直接统计分类偏好失败，使用备用方法: {}", e.getMessage());
            categoryPreferences = calculateCategoryPreferencesAlternative(userId, sinceTime);
        }

        return categoryPreferences;
    }

    /**
     * 备用的分类偏好计算方法
     */
    private Map<String, Long> calculateCategoryPreferencesAlternative(String userId, LocalDateTime sinceTime) {
        Map<String, Long> categoryPreferences = new HashMap<>();

        try {
            // 获取用户行为
            List<UserBehavior> behaviors = userBehaviorRepository
                    .findByUserIdAndBehaviorTimeAfter(userId, sinceTime);

            if (behaviors.isEmpty()) {
                return categoryPreferences;
            }

            // 提取新闻ID
            Set<String> newsIds = behaviors.stream()
                    .map(UserBehavior::getNewsId)
                    .collect(Collectors.toSet());

            // 批量查询新闻类别
            List<News> newsList = newsRepository.findAllById(newsIds);
            Map<String, String> newsCategoryMap = newsList.stream()
                    .collect(Collectors.toMap(
                            News::getNewsId,
                            news -> news.getCategory() != null ? news.getCategory() : "未知"
                    ));

            // 统计类别偏好（考虑行为权重）
            for (UserBehavior behavior : behaviors) {
                String newsId = behavior.getNewsId();
                String category = newsCategoryMap.getOrDefault(newsId, "未知");

                if (!"未知".equals(category)) {
                    long weight = getBehaviorWeight(behavior.getBehaviorType());
                    categoryPreferences.put(category,
                            categoryPreferences.getOrDefault(category, 0L) + weight);
                }
            }

        } catch (Exception e) {
            log.error("计算分类偏好失败: {}", e.getMessage());
        }

        return categoryPreferences;
    }

    /**
     * 行为权重定义
     */
    private long getBehaviorWeight(UserBehavior.BehaviorType behaviorType) {
        switch (behaviorType) {
            case VIEW: return 1;
            case CLICK: return 2;
            case LIKE: return 3;
            case COLLECT: return 5;
            default: return 1;
        }
    }

    /**
     * 实现：根据用户活跃时间计算时间偏好
     */
    private Map<String, Long> calculateTimePreferences(String userId, LocalDateTime sinceTime) {
        Map<String, Long> timePreferences = new HashMap<>();

        try {
            // 获取用户行为
            List<UserBehavior> behaviors = userBehaviorRepository
                    .findByUserIdAndBehaviorTimeAfter(userId, sinceTime);

            if (behaviors.isEmpty()) {
                return timePreferences;
            }
            // 按时间段统计
            for (UserBehavior behavior : behaviors) {
                LocalDateTime time = behavior.getBehaviorTime();
                int hour = time.getHour();

                String timeSlot;
                if (hour >= 6 && hour < 12) {
                    timeSlot = "morning";
                } else if (hour >= 12 && hour < 14) {
                    timeSlot = "noon";
                } else if (hour >= 14 && hour < 18) {
                    timeSlot = "afternoon";
                } else if (hour >= 18 && hour < 22) {
                    timeSlot = "evening";
                } else {
                    timeSlot = "night";
                }

                timePreferences.put(timeSlot, timePreferences.getOrDefault(timeSlot, 0L) + 1);
            }

        } catch (Exception e) {
            log.error("计算时间偏好失败: {}", e.getMessage());
        }

        return timePreferences;
    }

    private LocalDateTime getLastActiveTime(String userId) {
        return userBehaviorRepository.findRecentBehaviorsByUserId(
                        userId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(UserBehavior::getBehaviorTime)
                .orElse(null);
    }

    /**
     * 新增：批量获取新闻类别（用于Python算法）
     */
    @PostMapping("/batch-categories")
    @Operation(summary = "批量获取新闻类别")
    public ResponseEntity<ApiResponse<Map<String, String>>> getBatchNewsCategories(
            @RequestBody BatchNewsRequest request) {

        try {
            List<String> newsIds = request.getNewsIds();

            if (newsIds == null || newsIds.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(new HashMap<>()));
            }

            // 限制最大数量
            if (newsIds.size() > 100) {
                newsIds = newsIds.subList(0, 100);
            }

            // 批量查询新闻
            List<News> newsList = newsRepository.findAllById(newsIds);

            Map<String, String> categories = newsList.stream()
                    .collect(Collectors.toMap(
                            News::getNewsId,
                            news -> news.getCategory() != null ? news.getCategory() : "未知"
                    ));

            // 补充缺失的新闻
            for (String newsId : newsIds) {
                if (!categories.containsKey(newsId)) {
                    categories.put(newsId, "未知");
                }
            }

            log.info("批量获取 {} 个新闻的类别", categories.size());

            return ResponseEntity.ok(ApiResponse.success(categories));

        } catch (Exception e) {
            log.error("批量获取新闻类别失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量获取新闻类别失败: " + e.getMessage()));
        }
    }

    /**
     * 新增：获取推荐结果存储接口
     */
    @PostMapping("/recommendations")
    @Operation(summary = "接收Python算法生成的推荐结果")
    public ResponseEntity<ApiResponse<String>> receiveRecommendations(
            @RequestBody @Valid RecommendationBatchRequest request) {

        log.info("收到Python推荐结果，用户: {}, 推荐数: {}",
                request.getUserId(), request.getRecommendations().size());

        try {
            // 这里可以存储推荐结果到数据库，方便后续分析
            // 如果不需要存储，可以直接返回成功
            log.info("Python算法推荐结果接收成功");

            return ResponseEntity.ok(ApiResponse.success("推荐结果接收成功"));

        } catch (Exception e) {
            log.error("处理推荐结果失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("处理推荐结果失败: " + e.getMessage()));
        }
    }


}
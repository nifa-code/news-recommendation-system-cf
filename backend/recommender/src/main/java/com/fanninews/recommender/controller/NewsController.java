package com.fanninews.recommender.controller;
import com.fanninews.recommender.dto.response.ApiResponse;
import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.service.UserBehaviorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/v1/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {
    private final NewsRepository newsRepository;
    private final UserBehaviorService userBehaviorService;

    @GetMapping("/detail/{newsId}")
    @Operation(summary = "获取新闻详情")
    public ApiResponse<Map<String, Object>> getNewsDetail(
            @PathVariable String newsId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 1. 查询新闻实体
            News news = newsRepository.findById(newsId)
                    .orElseThrow(() -> new RuntimeException("新闻不存在：" + newsId));
            Map<String, Object> result = new HashMap<>();
            result.put("id", news.getNewsId());
            result.put("title", news.getTitle());
            result.put("category", news.getCategory());
            result.put("abstractText", news.getAbstractText());
            result.put("publishTime", news.getPublishTime());
            result.put("viewCount", news.getViewCount());
            result.put("recommendationScore", news.getHeatScore());
            // 图片相关字段（✅ 修复：调用正确的 getHasImages()）
            result.put("coverImageUrl", news.getCoverImageUrl());
            result.put("thumbnailUrl", news.getThumbnailUrl());
            result.put("imageUrls", news.getImageUrls());
            result.put("hasImages", news.getHasImages()); // Boolean类型，直接get，无需is
            result.put("imageCount", news.getImageCount());
            // 交互相关字段
            String userId = userDetails != null ? userDetails.getUsername() : null;
            result.put("likeCount", news.getLikeCount() == null ? 0 : news.getLikeCount());
            result.put("collectCount", news.getCollectCount() == null ? 0 : news.getCollectCount());
            result.put("liked", userId != null && userBehaviorService.checkUserLiked(userId, newsId));
            result.put("collected", userId != null && userBehaviorService.checkUserCollected(userId, newsId));
            // 3. 返回统一格式
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("获取新闻详情失败", e);
            return ApiResponse.error("获取新闻详情失" +
                    "【败：" + e.getMessage());
        }
    }
}
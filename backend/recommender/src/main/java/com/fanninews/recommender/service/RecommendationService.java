package com.fanninews.recommender.service;
import com.fanninews.recommender.dto.NewsConverter;
import com.fanninews.recommender.dto.request.PythonRecommendRequest;
import com.fanninews.recommender.dto.request.RecommendationRequest;
import com.fanninews.recommender.dto.NewsDTO;
import com.fanninews.recommender.dto.response.RecommendationDTO;
import com.fanninews.recommender.dto.response.RecommendationResponse;
import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.entity.PythonRecommendationItem;
import com.fanninews.recommender.entity.UserBehavior;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Data
public class RecommendationService {
    @Value("${python.api.url}")
    private String pythonApiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserBehaviorRepository userBehaviorRepository;
    private final NewsRepository newsRepository;
    private final NewsConverter newsConverter;
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        String userId = request.getUserId();
        int count = request.getCount();

        log.info("🚀 开始推荐处理 - 用户: {}, 数量: {}", userId, count);

        RecommendationResponse response = new RecommendationResponse();
        response.setUserId(userId);
        try {
            List<PythonRecommendationItem> pythonRecs = callPythonRecommendationAPI(userId, count*2);

            log.info("📊 从Python获取到 {} 条推荐", pythonRecs.size());

            if (pythonRecs.isEmpty()) {
                log.warn("Python推荐返回空，返回热门新闻");
                return getHotNewsResponse(userId, count, "协同过滤无结果");
            }

            // 2. 提取新闻ID
            List<String> recommendedIds = pythonRecs.stream()
                    .map(PythonRecommendationItem::getNewsId)
                    .collect(Collectors.toList());

            log.info("📋 推荐的新闻ID: {}", recommendedIds);

            // 3. 过滤已读新闻
            List<String> filteredIds = filterReadNews(recommendedIds, userId);
            if (filteredIds.isEmpty() && !recommendedIds.isEmpty()) {
                log.info("用户已阅读所有推荐新闻，不过滤");
                filteredIds = recommendedIds;
            }
            // 4. 限制数量
            List<String> finalIds = filteredIds.stream()
                    .limit(count)
                    .collect(Collectors.toList());

            log.info("🎯 最终推荐 {} 个新闻", finalIds.size());

            // 5. 从数据库获取新闻详情
            List<News> newsList = getNewsDetails(finalIds);
            if (newsList.isEmpty()) {
                log.warn("数据库中未找到推荐新闻，返回热门新闻");
                return getHotNewsResponse(userId, count, "数据库无对应新闻");
            }
            Collections.shuffle(newsList);
            log.info("📰 从数据库获取到 {} 条新闻详情", newsList.size());
            // 6. 构建推荐分数映射（key现在是String）
            Map<String, Double> scoreMap = pythonRecs.stream()
                    .collect(Collectors.toMap(
                            PythonRecommendationItem::getNewsId,
                            PythonRecommendationItem::getScore,
                            (v1, v2) -> v1
                    ));
            List<NewsDTO> newsDTOs = convertToNewsDTOs(newsList, scoreMap);
            response.setRecommendations(newsDTOs);
            response.setAlgorithm("COLLABORATIVE_FILTERING");
            response.setIsColdStart(false);
            logRecommendation(userId, finalIds, "user_cf");
            response.setMessage("Personalized recommendations based on collaborative filtering");

            log.info("✅ 推荐生成成功！用户: {}, 返回 {} 条新闻", userId, newsDTOs.size());

            return response;

        } catch (Exception e) {
            log.error("❌ 推荐处理失败: {}", e.getMessage(), e);
            return getHotNewsResponse(userId, count, "系统异常: " + e.getMessage());
        }
    }
    private void logRecommendation(String userId, List<String> newsIds, String algorithm) {
        try {
            log.info("📝 记录推荐日志 - 用户: {}, 算法: {}, 新闻数: {}",
                    userId, algorithm, newsIds.size());
            // 这里可以保存到数据库或发送到消息队列用于分析
        } catch (Exception e) {
            log.warn("记录推荐日志失败: {}", e.getMessage());
        }
    }

    private List<PythonRecommendationItem> callPythonRecommendationAPI(String userId, int count) {
        try {
            // 1. 构造POST请求URL（去掉原有的userId路径参数）
            String url = String.format("%s/recommend", pythonApiUrl);
            log.info("📞 调用Python协同过滤API(POST): {}", url);
            log.info("   用户: {}, 数量: {}", userId, count);

            // 2. 构造POST请求体（匹配Python的Request模型）
            PythonRecommendRequest requestBody = new PythonRecommendRequest();
            requestBody.setUserId(userId);
            requestBody.setTopK(count);  // 传入需要的推荐数量
            requestBody.setUseDiversity(true);

            // 3. 设置请求头（JSON格式）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<PythonRecommendRequest> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 5. 解析响应（适配Python返回的JSON结构）
                JsonNode root = objectMapper.readTree(response.getBody());

                // 检查是否成功（Python端未返回success字段，直接解析recommendations）
                JsonNode recommendationsNode = root.get("recommendations");
                if (recommendationsNode == null || !recommendationsNode.isArray()) {
                    log.warn("Python API返回的recommendations格式不正确");
                    return Collections.emptyList();
                }

                List<PythonRecommendationItem> items = new ArrayList<>();
                for (JsonNode recNode : recommendationsNode) {
                    try {
                        PythonRecommendationItem item = new PythonRecommendationItem();
                        JsonNode newsIdNode = recNode.get("news_id");
                        if (newsIdNode == null) {
                            log.warn("推荐项缺少news_id字段");
                            continue;
                        }
                        item.setNewsId(newsIdNode.asText());

                        // 分数字段（Python返回的是score）
                        JsonNode scoreNode = recNode.get("score");
                        if (scoreNode != null) {
                            item.setScore(scoreNode.asDouble());
                        } else {
                            item.setScore(0.5);
                        }

                        items.add(item);

                    } catch (Exception e) {
                        log.warn("解析推荐项失败: {}", e.getMessage());
                        continue;
                    }
                }

                log.info("✅ 从Python获取到 {} 条推荐", items.size());
                return items;

            } else {
                log.warn("Python API响应异常: {}", response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (Exception e) {
            log.error("调用Python API异常: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    /**
     * 增强的过滤方法：考虑用户历史行为
     */
    private List<String> filterAndSortNews(List<String> newsIds, String userId,
                                           Map<String, Double> scoreMap) {
        if (newsIds == null || newsIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 1. 获取用户已读新闻
            Set<String> readNewsIds = userBehaviorRepository.findViewedNewsIdsByUserId(userId);

            // 2. 获取用户偏好类别
            Map<String, Long> categoryPreferences = getUserCategoryPreferences(userId);

            // 3. 获取新闻类别信息
            List<News> newsList = newsRepository.findAllById(newsIds);
            Map<String, String> newsCategoryMap = newsList.stream()
                    .collect(Collectors.toMap(
                            News::getNewsId,
                            news -> news.getCategory() != null ? news.getCategory() : "未知"
                    ));

            // 4. 综合排序：推荐分数 + 类别偏好 - 已读惩罚
            List<String> sortedIds = newsIds.stream()
                    .sorted((id1, id2) -> {
                        // 基础分数
                        double score1 = scoreMap.getOrDefault(id1, 0.0);
                        double score2 = scoreMap.getOrDefault(id2, 0.0);

                        // 已读惩罚
                        if (readNewsIds.contains(id1)) score1 *= 0.1;
                        if (readNewsIds.contains(id2)) score2 *= 0.1;

                        // 类别偏好加成
                        String category1 = newsCategoryMap.get(id1);
                        String category2 = newsCategoryMap.get(id2);

                        if (category1 != null && categoryPreferences.containsKey(category1)) {
                            score1 *= (1 + categoryPreferences.get(category1) * 0.3);
                        }
                        if (category2 != null && categoryPreferences.containsKey(category2)) {
                            score2 *= (1 + categoryPreferences.get(category2) * 0.3);
                        }

                        return Double.compare(score2, score1); // 降序
                    })
                    .collect(Collectors.toList());

            log.info("📊 综合排序完成，已过滤 {} 条已读新闻",
                    newsIds.size() - sortedIds.size());

            return sortedIds;

        } catch (Exception e) {
            log.warn("综合排序失败，使用原始顺序: {}", e.getMessage());
            return newsIds;
        }
    }

    /**
     * 获取用户类别偏好
     */
    private Map<String, Long> getUserCategoryPreferences(String userId) {
        try {
            // 获取用户最近30天的行为
            LocalDateTime sinceTime = LocalDateTime.now().minusDays(30);

            // 查询用户阅读的新闻
            List<UserBehavior> behaviors = userBehaviorRepository
                    .findByUserIdAndBehaviorTimeAfter(userId, sinceTime);

            // 提取新闻ID
            Set<String> newsIds = behaviors.stream()
                    .map(UserBehavior::getNewsId)
                    .collect(Collectors.toSet());

            if (newsIds.isEmpty()) {
                return Collections.emptyMap();
            }

            // 获取这些新闻的类别
            List<News> newsList = newsRepository.findAllById(newsIds);

            // 统计类别偏好（考虑行为权重）
            Map<String, Long> categoryCounts = new HashMap<>();
            for (UserBehavior behavior : behaviors) {
                News news = newsList.stream()
                        .filter(n -> n.getNewsId().equals(behavior.getNewsId()))
                        .findFirst()
                        .orElse(null);

                if (news != null && news.getCategory() != null) {
                    String category = news.getCategory();
                    long weight = getBehaviorWeight(behavior.getBehaviorType());

                    categoryCounts.put(category,
                            categoryCounts.getOrDefault(category, 0L) + weight);
                }
            }

            return categoryCounts;

        } catch (Exception e) {
            log.warn("获取用户类别偏好失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

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
     * 过滤已读新闻 - newsId为String版本
     */
    private List<String> filterReadNews(List<String> newsIds, String userId) {
        try {
            // 获取用户已读新闻ID（String类型）
            Set<String> readNewsIds = userBehaviorRepository.findViewedNewsIdsByUserId(userId);

            if (readNewsIds == null || readNewsIds.isEmpty()) {
                log.info("用户 {} 没有已读记录", userId);
                return new ArrayList<>(newsIds);
            }

            log.info("用户 {} 有 {} 条已读记录", userId, readNewsIds.size());

            // 过滤已读新闻
            List<String> filtered = newsIds.stream()
                    .filter(newsId -> !readNewsIds.contains(newsId))
                    .collect(Collectors.toList());

            log.info("过滤后剩余 {} 条新闻（原始 {} 条）", filtered.size(), newsIds.size());

            return filtered;

        } catch (Exception e) {
            log.warn("过滤已读新闻失败: {}", e.getMessage());
            return new ArrayList<>(newsIds);
        }
    }
    /**
     * 获取新闻详情 - newsId为String版本
     */
    private List<News> getNewsDetails(List<String> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<News> newsList = newsRepository.findAllById(newsIds);

            // 按原始ID顺序排序
            Map<String, News> newsMap = newsList.stream()
                    .collect(Collectors.toMap(News::getNewsId, news -> news));

            return newsIds.stream()
                    .filter(newsMap::containsKey)
                    .map(newsMap::get)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取新闻详情失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<NewsDTO> convertToNewsDTOs(List<News> newsList, Map<String, Double> scoreMap) {
        // 一行代码替换原有几十行转换逻辑
        return newsConverter.toDTOList(newsList, scoreMap);
    }
    /**
     * 获取热门新闻响应,从数据库中获取
     */
    /**
     * 冷启动：热门 + 随机混合推荐（已修复重复新闻问题）
     */
    public RecommendationResponse getHotNewsResponse(String userId, int count, String reason) {
        log.warn("🔄 冷启动：热门+随机混合推荐 | 用户:{} 数量:{}", userId, count);
        try {
            int hotCount = (int) (count * 0.4);
            int randomCount = count - hotCount;

            List<News> hotNews = newsRepository.findTopByOrderByViewCountDesc(PageRequest.of(0, hotCount));
            List<News> randomNews = newsRepository.findRandomNews(randomCount);

            // ====================== 【修复】合并并自动去重 ======================
            Map<String, News> uniqueMap = new HashMap<>();

            // 先放热门
            for (News n : hotNews) uniqueMap.put(n.getNewsId(), n);
            // 再放随机（自动覆盖重复）
            for (News n : randomNews) uniqueMap.put(n.getNewsId(), n);

            // 转成列表
            List<News> mixed = new ArrayList<>(uniqueMap.values());
            Collections.shuffle(mixed);

            // 如果数量不够，再补随机
            while (mixed.size() < count) {
                List<News> more = newsRepository.findRandomNews(count - mixed.size());
                for (News n : more) {
                    if (!uniqueMap.containsKey(n.getNewsId())) {
                        uniqueMap.put(n.getNewsId(), n);
                        mixed.add(n);
                    }
                }
            }

            List<NewsDTO> dtos = convertToNewsDTOs(mixed, new HashMap<>());
            RecommendationResponse response = new RecommendationResponse();
            response.setUserId(userId);
            response.setRecommendations(dtos);
            response.setAlgorithm("HOT_RANDOM_MIX");
            response.setIsColdStart(true);
            response.setMessage("新用户推荐：热门+随机混合");
            return response;

        } catch (Exception e) {
            log.error("混合推荐失败", e);
            return createMockResponse(userId, count);
        }
    }
//    public RecommendationResponse getHotNewsResponse(String userId, int count, String reason) {
//        log.warn("🔄 使用降级策略: {}", reason);
//
//        try {
//            List<News> hotNews = newsRepository.findTopByOrderByViewCountDesc(
//                    PageRequest.of(0, count*2)
//            );
//
//            if (hotNews.isEmpty()) {
//                hotNews = newsRepository.findRandomNews(count);
//            }
//            List<NewsDTO>newsDTOs=convertToNewsDTOs(hotNews, new HashMap<>());
//            RecommendationResponse response = new RecommendationResponse();
//            response.setUserId(userId);
//            response.setRecommendations(newsDTOs);
//            response.setAlgorithm("HOT");
//            response.setIsColdStart(true);
//            response.setMessage("降级: " + reason);
//
//            return response;
//
//        } catch (Exception e) {
//            log.error("降级策略失败: {}", e.getMessage());
//            return createMockResponse(userId, count);
//        }
//    }

    private List<RecommendationDTO> getSimilarNewsRecommendations(String newsId, int topK) {
        try {
            Optional<News> currentNews = newsRepository.findById(newsId);
            if (currentNews.isPresent()) {
                String category = currentNews.get().getCategory();

                // 使用Pageable限制数量
                Pageable pageable = PageRequest.of(0, topK, Sort.by(Sort.Direction.DESC, "publishTime"));

                List<News> similarNews = newsRepository.findByCategoryExcludingId(
                        category, newsId, pageable);

                return similarNews.stream()
                        .map(news -> {
                            RecommendationDTO dto = new RecommendationDTO();
                            dto.setNewsId(news.getNewsId());
                            dto.setTitle(news.getTitle());
                            dto.setSummary(news.getAbstractText());
                            dto.setImageUrl(news.getImageUrls());
                            dto.setCategory(news.getCategory());
                            dto.setScore(0.7);
                            return dto;
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("获取相似新闻失败: {}", e.getMessage());
        }
        return new ArrayList<>();
    }
    /**
     * 创建模拟响应
     */
    private RecommendationResponse createMockResponse(String userId, int count) {
        List<NewsDTO> mockNews = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            NewsDTO dto = new NewsDTO();
            dto.setId("mock_" + i);  // String类型ID
            dto.setTitle("示例新闻 " + i);
            dto.setAbstractText("这是示例数据，请检查Python服务和数据库连接");
            dto.setCategory("示例");
            dto.setViewCount(100 + i * 10);
            dto.setRecommendationScore(0.3 + i * 0.05);
            dto.setPublishTime(LocalDateTime.now().minusDays(i));
            mockNews.add(dto);
        }
        RecommendationResponse response = new RecommendationResponse();
        response.setUserId(userId);
        response.setRecommendations(mockNews);
        response.setAlgorithm("MOCK");
        response.setIsColdStart(true);
        response.setMessage("系统异常，使用示例数据");

        return response;
    }
    // ===== 工具方法 =====
    private String getStringValue(Object obj) {
        return obj != null ? obj.toString() : "";
    }


    private <T> T ensureNotNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}



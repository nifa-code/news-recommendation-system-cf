package com.fanninews.recommender.dto;
import com.fanninews.recommender.dto.NewsDTO;
import com.fanninews.recommender.entity.News;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NewsConverter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 基础转换（无推荐分数）
    public NewsDTO toDTO(News news) {
        return toDTO(news, null);
    }

    // 扩展：支持传入推荐分数（适配推荐接口）
    public NewsDTO toDTO(News news, Double recommendationScore) {
        if (news == null) {
            return null;
        }

        NewsDTO newsDTO = new NewsDTO();
        // 基础字段（和之前一致）
        newsDTO.setId(news.getNewsId());
        newsDTO.setTitle(ensureNotNull(news.getTitle(), "无标题"));
        //newsDTO.setAbstractText(news.getAbstractText());
        String abs = news.getAbstractText();
        if (abs != null && abs.length() > 150) {
            abs = abs.substring(0, 150) + "...";
        }
        newsDTO.setAbstractText(abs);
        newsDTO.setCategory(ensureNotNull(news.getCategory(), "未分类"));
        newsDTO.setPublishTime(news.getPublishTime() != null ? news.getPublishTime() : LocalDateTime.now().minusDays(1));
        newsDTO.setViewCount(ensureNotNull(news.getViewCount(), 0));

        // 图片字段解析（复用原有逻辑）
        newsDTO.setCoverImageUrl(news.getCoverImageUrl());
        newsDTO.setThumbnailUrl(news.getThumbnailUrl());
        newsDTO.setHasImages(news.getHasImages());
        newsDTO.setImageCount(news.getImageCount());
        if (news.getImageUrls() != null && !news.getImageUrls().isEmpty()) {
            try {
                List<String> urls = objectMapper.readValue(
                        news.getImageUrls(),
                        new TypeReference<List<String>>() {}
                );
                newsDTO.setImageUrls(urls);
            } catch (JsonProcessingException e) {
                log.warn("解析imageUrls失败（新闻ID：{}）：{}", news.getNewsId(), e.getMessage());
                newsDTO.setImageUrls(new ArrayList<>());
            }
        } else {
            newsDTO.setImageUrls(new ArrayList<>());
        }

        // 推荐分数（新增）
        newsDTO.setRecommendationScore(recommendationScore != null ? recommendationScore : 0.5);

        return newsDTO;
    }

    // 批量转换（适配推荐接口，支持分数映射）
    public List<NewsDTO> toDTOList(List<News> newsList, Map<String, Double> scoreMap) {
        if (newsList == null || newsList.isEmpty()) {
            return new ArrayList<>();
        }
        return newsList.stream()
                .map(news -> toDTO(news, scoreMap.getOrDefault(news.getNewsId(), 0.5)))
                .collect(Collectors.toList());
    }

    // 批量转换（无推荐分数）
    public List<NewsDTO> toDTOList(List<News> newsList) {
        return toDTOList(newsList, new HashMap<>());
    }

    // 工具方法：确保非空
    private <T> T ensureNotNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }
}
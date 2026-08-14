package com.fanninews.recommender.service.HotNewsUpdater;

import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HotNewsUpdater {

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private UserBehaviorRepository userBehaviorRepository;

    /**
     * 每小时更新一次热门新闻
     */
    @Scheduled(cron = "0 0 * * * ?")  // 每小时执行一次
    @Transactional
    public void updateHotNews() {
        log.info("开始更新热门新闻...");
        LocalDateTime sinceTime = LocalDateTime.now().minusHours(24);
        Pageable top100 = PageRequest.of(0, 100);
        List<Object[]> hotNewsResults = userBehaviorRepository.findNewsBehaviorStats(sinceTime);
        Map<String, Double> newsHeatMap = new HashMap<>();
        for (Object[] result : hotNewsResults) {
            String newsId = (String) result[0];
            String behaviorType = (String) result[1];
            Long count = (Long) result[2];

            double weight = getBehaviorWeight(behaviorType);
            newsHeatMap.put(newsId, newsHeatMap.getOrDefault(newsId, 0.0) + count * weight);
        }
        List<News> allNews = newsRepository.findAll();
        for (News news : allNews) {
            double heatScore = newsHeatMap.getOrDefault(news.getNewsId(), 0.0);
            news.setHeatScore(heatScore);
            news.setIsHot(heatScore > 5.0); 

            newsRepository.save(news);
        }

        log.info("热门新闻更新完成，共处理 {} 条新闻", allNews.size());
    }
    private double getBehaviorWeight(String behaviorType) {
        switch (behaviorType) {
            case "LIKE": return 3.0;
            case "COLLECT": return 5.0;
            case "VIEW": return 0.5;
            default: return 0.1;
        }
    }
}

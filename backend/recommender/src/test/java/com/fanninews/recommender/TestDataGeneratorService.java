package com.fanninews.recommender;
import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.entity.UserBehavior;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestDataGeneratorService {

    private final UserBehaviorRepository userBehaviorRepository;
    private final NewsRepository newsRepository;

    /**
     * 快速生成测试数据（用于立即测试）
     */
    @Transactional
    public void generateQuickTestData() {
        log.info("🎲 开始生成快速测试数据...");

        // 1. 先删除旧数据（可选）
        userBehaviorRepository.deleteAll();
        log.info("🗑️  已清除旧的行为数据");

        // 2. 获取所有新闻
        List<News> allNews = newsRepository.findAll();
        if (allNews.isEmpty()) {
            log.error("❌ 新闻表为空，请先导入新闻数据！");
            return;
        }

        log.info("📰 找到 {} 条新闻", allNews.size());

        // 3. 生成用户行为
        List<UserBehavior> behaviors = new ArrayList<>();
        Random random = new Random();

        // 生成1000个用户，每个用户10-50条行为
        for (int userNum = 1; userNum <= 1000; userNum++) {
            String userId = "test_user_" + String.format("%04d", userNum);

            int behaviorCount = 10 + random.nextInt(40); // 10-50条行为
            Set<String> interactedNews = new HashSet<>();

            for (int i = 0; i < behaviorCount; i++) {
                // 随机选择一条新闻
                News news = allNews.get(random.nextInt(allNews.size()));

                // 避免重复交互
                if (interactedNews.contains(news.getNewsId())) {
                    continue;
                }
                interactedNews.add(news.getNewsId());

                // 创建行为记录
                UserBehavior behavior = new UserBehavior();
                behavior.setUserId(userId);
                behavior.setNewsId(news.getNewsId());

                // 随机行为类型（权重不同）
                int behaviorType = random.nextInt(100);
                if (behaviorType < 60) { // 60% 浏览
                    behavior.setBehaviorType(UserBehavior.BehaviorType.VIEW);
                } else if (behaviorType < 85) { // 25% 点击
                    behavior.setBehaviorType(UserBehavior.BehaviorType.CLICK);
                } else if (behaviorType < 95) { // 10% 点赞
                    behavior.setBehaviorType(UserBehavior.BehaviorType.LIKE);
                } else { // 5% 收藏
                    behavior.setBehaviorType(UserBehavior.BehaviorType.COLLECT);
                }

                // 随机时间（2017年内，与新闻时间匹配）
                LocalDateTime publishTime = news.getPublishTime();
                if (publishTime != null) {
                    // 在新闻发布后的1-30天内产生行为
                    int daysAfter = 1 + random.nextInt(30);
                    behavior.setBehaviorTime(publishTime.plusDays(daysAfter));
                } else {
                    // 如果没有发布时间，使用2017年随机时间
                    LocalDateTime start2017 = LocalDateTime.of(2017, 1, 1, 0, 0);
                    int day = random.nextInt(365);
                    int hour = random.nextInt(24);
                    behavior.setBehaviorTime(start2017.plusDays(day).plusHours(hour));
                }

                // 随机时长（0-300秒）
                behavior.setDurationSeconds(random.nextInt(300));

                behaviors.add(behavior);
            }

            if (userNum % 100 == 0) {
                log.info("已生成 {} 个用户的行为数据", userNum);
            }

            // 批量保存，每500条保存一次
            if (behaviors.size() >= 500) {
                userBehaviorRepository.saveAll(behaviors);
                behaviors.clear();
            }
        }

        // 保存剩余数据
        if (!behaviors.isEmpty()) {
            userBehaviorRepository.saveAll(behaviors);
        }

        // 统计信息
        long totalBehaviors = userBehaviorRepository.count();
        long distinctUsers = userBehaviorRepository.countDistinctUsers();

        log.info("✅ 测试数据生成完成！");
        log.info("📊 数据统计：");
        log.info("   用户数：{}", distinctUsers);
        log.info("   行为记录数：{}", totalBehaviors);
        log.info("   平均每个用户行为数：{}",
                distinctUsers > 0 ? totalBehaviors / distinctUsers : 0);
    }

    /**
     * 生成热门新闻数据（更新新闻的热度）
     */
    @Transactional
    public void generateNewsHeatData() {
        log.info("🔥 开始生成新闻热度数据...");

        // 随机给一些新闻添加热度
        List<News> allNews = newsRepository.findAll();
        Random random = new Random();

        for (News news : allNews) {
            // 随机生成热度数据
            news.setViewCount(random.nextInt(10000));
            news.setLikeCount(random.nextInt(1000));
            news.setCollectCount(random.nextInt(500));

            // 计算热度分数
            double heatScore =
                    news.getViewCount() * 0.1 +
                            news.getLikeCount() * 0.3 +
                            news.getCollectCount() * 0.5;

            // 时间衰减因子（2017年的新闻）
            if (news.getPublishTime() != null) {
                long daysSince2017 = 365 * 3; // 假设现在是2020年
                double timeDecay = Math.exp(-daysSince2017 / 365.0);
                heatScore *= timeDecay;
            }

            news.setHeatScore(heatScore);
            news.setIsHot(heatScore > 50); // 阈值50

            // 随机设置一些图片URL
            if (random.nextBoolean()) {
                news.setCoverImageUrl("https://example.com/cover_" +
                        news.getNewsId().substring(0, 5) + ".jpg");
                news.setHasImages(true);
                news.setImageCount(1 + random.nextInt(5));
            }
        }

        newsRepository.saveAll(allNews);

        long hotNewsCount = allNews.stream()
                .filter(News::getIsHot)
                .count();

        log.info("✅ 新闻热度数据生成完成！");
        log.info("📊 热门新闻数：{}/{}", hotNewsCount, allNews.size());
    }
}
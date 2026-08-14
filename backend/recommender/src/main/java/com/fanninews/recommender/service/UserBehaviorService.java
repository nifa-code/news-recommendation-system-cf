package com.fanninews.recommender.service;
import com.fanninews.recommender.Exception.NotFoundException;
import com.fanninews.recommender.dto.NewsInteractionDTO;
import com.fanninews.recommender.dto.UserBehaviorDTO;
import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.entity.UserBehavior;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.repository.UserBehaviorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
@Slf4j
public class UserBehaviorService {
    private final UserBehaviorRepository userBehaviorRepository;
    private final NewsRepository newsRepository;
    private final RabbitTemplate rabbitTemplate;

    // 统一使用构造器注入（最佳实践）
    @Autowired
    public UserBehaviorService(
            UserBehaviorRepository userBehaviorRepository,
            NewsRepository newsRepository,
            RabbitTemplate rabbitTemplate) {
        this.userBehaviorRepository = userBehaviorRepository;
        this.newsRepository = newsRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 重新计算新闻热度分数
     */
    private void recalculateHeatScore(News news) {
        // 热度计算公式: heat = 浏览数*0.1 + 点赞数*0.3 + 收藏数*0.5 + 时间衰减因子
        double viewWeight = 0.1;
        double likeWeight = 0.3;
        double collectWeight = 0.5;

        int views = news.getViewCount() != null ? news.getViewCount() : 0;
        int likes = news.getLikeCount() != null ? news.getLikeCount() : 0;
        int collects = news.getCollectCount() != null ? news.getCollectCount() : 0;

        // 时间衰减因子（新闻越新，分数越高）
        LocalDateTime publishTime = news.getPublishTime();
        if (publishTime != null) {
            long hoursSincePublish = Duration.between(publishTime, LocalDateTime.now()).toHours();
            double timeDecay = Math.exp(-hoursSincePublish / 168.0); // 按周衰减
            double baseScore = views * viewWeight + likes * likeWeight + collects * collectWeight;
            news.setHeatScore(baseScore * timeDecay);
        } else {
            news.setHeatScore(views * viewWeight + likes * likeWeight + collects * collectWeight);
        }

        // 设置热门标记（例如热度超过阈值）
        news.setIsHot(news.getHeatScore() > 10.0);
    }

    /**
     * 记录用户行为（通用方法）
     * 对于 VIEW 行为，确保不会重复记录
     */
    @Transactional
    public void recordUserBehavior(String userId, String newsId,
                                   UserBehavior.BehaviorType behaviorType,
                                   Integer durationSeconds) {
        try {
            // 对于 VIEW 行为，检查是否已经存在（避免重复记录）
//            if (behaviorType == UserBehavior.BehaviorType.VIEW) {
//                boolean alreadyExists = userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
//                        userId, newsId, UserBehavior.BehaviorType.VIEW);
//                if (alreadyExists) {
//                    log.debug("VIEW行为已存在，跳过记录: userId={}, newsId={}", userId, newsId);
//                    return;
//                }
//            }

            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setNewsId(newsId);
            behavior.setBehaviorType(behaviorType);
            behavior.setBehaviorTime(LocalDateTime.now());

            if (durationSeconds != null) {
                behavior.setDurationSeconds(durationSeconds);
            }

            userBehaviorRepository.save(behavior);

            // 根据行为类型更新新闻统计
            switch (behaviorType) {
                case LIKE:
                    newsRepository.incrementLikeCount(newsId);
                    break;
                case COLLECT:
                    newsRepository.incrementCollectCount(newsId);
                    break;
                case VIEW:
                    newsRepository.incrementViewCount(newsId);
                    break;
                case CLICK:
                    // CLICK行为通常不更新统计，或者可以记录为预览
                    break;
                default:
                    // 其他行为不更新新闻统计
                    break;
            }

            // 可选：重新计算新闻热度
            newsRepository.findById(newsId).ifPresent(this::recalculateHeatScore);

            log.info("用户行为记录成功: userId={}, newsId={}, type={}",
                    userId, newsId, behaviorType);
        } catch (Exception e) {
            log.error("记录用户行为失败", e);
            throw new RuntimeException("记录用户行为失败: " + e.getMessage());
        }
    }

    /**
     * 记录点击行为（用于列表页点击）
     */
    public void recordClick(String userId, String newsId) {
        boolean exists = userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
                userId, newsId, UserBehavior.BehaviorType.CLICK
        );
        if (exists) {
            log.info("点击行为已存在，跳过：{} {}", userId, newsId);
            return;
        }
        recordUserBehavior(userId, newsId, UserBehavior.BehaviorType.CLICK, null);
    }

    /**
     * 记录阅读行为（用于详情页阅读）
     */
    public void recordRead(String userId, String newsId) {
        boolean exists = userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
                userId, newsId, UserBehavior.BehaviorType.VIEW
        );
        if (exists) {
            log.info("点击行为已存在，跳过：{} {}", userId, newsId);
            return;
        }
        recordUserBehavior(userId, newsId, UserBehavior.BehaviorType.VIEW, null);
    }

    /**
     * 切换点赞状态
     */
    @Transactional
    public boolean toggleLike(String userId, String newsId) {
        try {
            UserBehavior existing = userBehaviorRepository
                    .findByUserIdAndNewsIdAndBehaviorType(userId, newsId, UserBehavior.BehaviorType.LIKE)
                    .orElse(null);

            if (existing != null) {
                // 已点赞，取消点赞
                userBehaviorRepository.delete(existing);
                newsRepository.decrementLikeCount(newsId);
                log.info("用户取消点赞: userId={}, newsId={}", userId, newsId);
                return false;
            } else {
                // 未点赞，执行点赞
                UserBehavior behavior = new UserBehavior();
                behavior.setUserId(userId);
                behavior.setNewsId(newsId);
                behavior.setBehaviorType(UserBehavior.BehaviorType.LIKE);
                behavior.setBehaviorTime(LocalDateTime.now());
                userBehaviorRepository.save(behavior);
                newsRepository.incrementLikeCount(newsId);
                log.info("用户点赞成功: userId={}, newsId={}", userId, newsId);
                return true;
            }
        } catch (Exception e) {
            log.error("切换点赞状态失败", e);
            throw new RuntimeException("切换点赞状态失败: " + e.getMessage());
        }
    }

    /**
     * 切换收藏状态
     */
    @Transactional
    public boolean toggleCollect(String userId, String newsId) {
        try {
            UserBehavior existing = userBehaviorRepository
                    .findByUserIdAndNewsIdAndBehaviorType(userId, newsId, UserBehavior.BehaviorType.COLLECT)
                    .orElse(null);

            if (existing != null) {
                // 已收藏，取消收藏
                userBehaviorRepository.delete(existing);
                newsRepository.decrementCollectCount(newsId);
                log.info("用户取消收藏: userId={}, newsId={}", userId, newsId);
                return false;
            } else {
                // 未收藏，执行收藏
                UserBehavior behavior = new UserBehavior();
                behavior.setUserId(userId);
                behavior.setNewsId(newsId);
                behavior.setBehaviorType(UserBehavior.BehaviorType.COLLECT);
                behavior.setBehaviorTime(LocalDateTime.now());
                userBehaviorRepository.save(behavior);
                newsRepository.incrementCollectCount(newsId);
                log.info("用户收藏成功: userId={}, newsId={}", userId, newsId);
                return true;
            }
        } catch (Exception e) {
            log.error("切换收藏状态失败", e);
            throw new RuntimeException("切换收藏状态失败: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否点赞过某新闻
     */
    public boolean checkUserLiked(String userId, String newsId) {
        return userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
                userId, newsId, UserBehavior.BehaviorType.LIKE);
    }

    /**
     * 检查用户是否收藏过某新闻
     */
    public boolean checkUserCollected(String userId, String newsId) {
        return userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
                userId, newsId, UserBehavior.BehaviorType.COLLECT);
    }

    /**
     * 检查用户是否阅读过某新闻（VIEW 行为）
     */
    public boolean checkUserRead(String userId, String newsId) {
        return userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
                userId, newsId, UserBehavior.BehaviorType.VIEW);
    }

    /**
     * 检查用户是否点击过某新闻（CLICK 行为）
     */
    public boolean checkUserClicked(String userId, String newsId) {
        return userBehaviorRepository.existsByUserIdAndNewsIdAndBehaviorType(
                userId, newsId, UserBehavior.BehaviorType.CLICK);
    }

    /**
     * 获取新闻的点赞数
     */
    public Integer getNewsLikeCount(String newsId) {
        return newsRepository.findById(newsId)
                .map(News::getLikeCount)
                .orElse(0);
    }

    /**
     * 获取新闻的收藏数
     */
    public Integer getNewsCollectCount(String newsId) {
        return newsRepository.findById(newsId)
                .map(News::getCollectCount)
                .orElse(0);
    }

    /**
     * 获取新闻的浏览量
     */
    public Integer getNewsViewCount(String newsId) {
        return newsRepository.findById(newsId)
                .map(News::getViewCount)
                .orElse(0);
    }

    private UserBehaviorDTO convertToDTO(UserBehavior behavior) {
        UserBehaviorDTO dto = new UserBehaviorDTO();
        dto.setId(behavior.getId());
        dto.setUserId(behavior.getUserId());
        dto.setNewsId(behavior.getNewsId());
        dto.setBehaviorType(behavior.getBehaviorType());
        dto.setBehaviorTime(behavior.getBehaviorTime());
        dto.setDurationSeconds(behavior.getDurationSeconds());
        return dto;
    }

    /**
     * 获取用户的行为状态（综合信息）
     */
    public Map<String, Boolean> getUserBehaviorStatus(String userId, String newsId) {
        Map<String, Boolean> status = new HashMap<>();
        status.put("liked", checkUserLiked(userId, newsId));
        status.put("collected", checkUserCollected(userId, newsId));
        status.put("read", checkUserRead(userId, newsId));
        status.put("clicked", checkUserClicked(userId, newsId));
        return status;
    }

    /**
     * 获取用户的所有点赞新闻
     */
    public List<String> getUserLikedNewsIds(String userId) {
        List<UserBehavior> likes = userBehaviorRepository.findByUserIdAndBehaviorType(
                userId, UserBehavior.BehaviorType.LIKE);
        return likes.stream()
                .map(UserBehavior::getNewsId)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户的所有收藏新闻
     */
    public List<String> getUserCollectedNewsIds(String userId) {
        List<UserBehavior> collects = userBehaviorRepository.findByUserIdAndBehaviorType(
                userId, UserBehavior.BehaviorType.COLLECT);
        return collects.stream()
                .map(UserBehavior::getNewsId)
                .collect(Collectors.toList());
    }




    /**
     * 获取用户点赞列表（带新闻详情）
     */
    public List<Map<String, Object>> getUserLikeList(String userId, int pageNum, int pageSize) {
        try {
            org.springframework.data.domain.Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
            List<UserBehavior> likeBehaviors = userBehaviorRepository.findByUserIdAndBehaviorTypeOrderByBehaviorTimeDesc(
                    userId, UserBehavior.BehaviorType.LIKE, pageable);

            List<Map<String, Object>> likeList = new ArrayList<>();
            for (UserBehavior behavior : likeBehaviors) {
                String newsId = behavior.getNewsId();
                News news = newsRepository.findById(newsId).orElse(null);

                if (news != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("behaviorId", behavior.getId());
                    item.put("likeTime", behavior.getBehaviorTime());
                    item.put("newsId", news.getNewsId());
                    item.put("title", news.getTitle());
                    item.put("category", news.getCategory());
                    item.put("coverImageUrl", news.getCoverImageUrl());
                    item.put("publishTime", news.getPublishTime());
                    item.put("abstractText", news.getAbstractText());
                    item.put("likeCount", news.getLikeCount() == null ? 0 : news.getLikeCount());
                    item.put("collectCount", news.getCollectCount() == null ? 0 : news.getCollectCount());

                    likeList.add(item);
                }
            }
            return likeList;
        } catch (Exception e) {
            log.error("获取用户点赞列表失败", e);
            throw new RuntimeException("获取点赞列表失败");
        }
    }

    /**
     * 获取用户浏览历史（带新闻详情），支持分页
     */
    public List<Map<String, Object>> getUserViewHistory(String userId, int pageNum, int pageSize) {
        try {
            // 分页查询用户的VIEW行为（pageNum从1开始，PageRequest从0开始）
            org.springframework.data.domain.Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
            List<UserBehavior> viewBehaviors = userBehaviorRepository.findByUserIdAndBehaviorTypeOrderByBehaviorTimeDesc(
                    userId, UserBehavior.BehaviorType.VIEW, pageable);

            // 转换为带新闻详情的结果
            List<Map<String, Object>> historyList = new ArrayList<>();
            for (UserBehavior behavior : viewBehaviors) {
                String newsId = behavior.getNewsId();
                News news = newsRepository.findById(newsId).orElse(null);

                if (news != null) {
                    Map<String, Object> item = new HashMap<>();
                    // 行为相关信息
                    item.put("behaviorId", behavior.getId());
                    item.put("behaviorTime", behavior.getBehaviorTime());
                    item.put("durationSeconds", behavior.getDurationSeconds());
                    // 新闻相关信息
                    item.put("newsId", news.getNewsId());
                    item.put("title", news.getTitle());
                    item.put("category", news.getCategory());
                    item.put("coverImageUrl", news.getCoverImageUrl());
                    item.put("publishTime", news.getPublishTime());
                    item.put("abstractText", news.getAbstractText());

                    historyList.add(item);
                }
            }
            return historyList;
        } catch (Exception e) {
            log.error("获取用户浏览历史失败: userId={}", userId, e);
            throw new RuntimeException("获取浏览历史失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户收藏列表（带新闻详情），支持分页
     */
    public List<Map<String, Object>> getUserCollectList(String userId, int pageNum, int pageSize) {
        try {
            org.springframework.data.domain.Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
            List<UserBehavior> collectBehaviors = userBehaviorRepository.findByUserIdAndBehaviorTypeOrderByBehaviorTimeDesc(
                    userId, UserBehavior.BehaviorType.COLLECT, pageable);

            List<Map<String, Object>> collectList = new ArrayList<>();
            for (UserBehavior behavior : collectBehaviors) {
                String newsId = behavior.getNewsId();
                News news = newsRepository.findById(newsId).orElse(null);

                if (news != null) {
                    Map<String, Object> item = new HashMap<>();
                    // 行为相关信息
                    item.put("behaviorId", behavior.getId());
                    item.put("collectTime", behavior.getBehaviorTime());
                    // 新闻相关信息
                    item.put("newsId", news.getNewsId());
                    item.put("title", news.getTitle());
                    item.put("category", news.getCategory());
                    item.put("coverImageUrl", news.getCoverImageUrl());
                    item.put("publishTime", news.getPublishTime());
                    item.put("abstractText", news.getAbstractText());
                    item.put("likeCount", news.getLikeCount() == null ? 0 : news.getLikeCount());
                    item.put("collectCount", news.getCollectCount() == null ? 0 : news.getCollectCount());

                    collectList.add(item);
                }
            }
            return collectList;
        } catch (Exception e) {
            log.error("获取用户收藏列表失败: userId={}", userId, e);
            throw new RuntimeException("获取收藏列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户浏览历史总数（用于分页）
     */
    public long getUserViewHistoryCount(String userId) {
        return userBehaviorRepository.countByUserIdAndBehaviorType(
                userId, UserBehavior.BehaviorType.VIEW);
    }

    /**
     * 获取用户收藏列表总数（用于分页）
     */
    public long getUserCollectListCount(String userId) {
        return userBehaviorRepository.countByUserIdAndBehaviorType(
                userId, UserBehavior.BehaviorType.COLLECT);
    }

    public long getUserLikeListCount(String userId) {
        return userBehaviorRepository.countByUserIdAndBehaviorType(
                userId, UserBehavior.BehaviorType.LIKE);

    }

// 补充countBy方法（如果Repository没有自动生成，需要手动在Repository添加）
// 在UserBehaviorRepository中新增：
// long countByUserIdAndBehaviorType(String userId, UserBehavior.BehaviorType behaviorType);
}
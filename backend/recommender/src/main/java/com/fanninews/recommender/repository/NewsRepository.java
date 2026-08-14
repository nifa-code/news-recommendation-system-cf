package com.fanninews.recommender.repository;

import com.fanninews.recommender.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, String> {
    // 最新新闻（按发布时间倒序）
    @Query("SELECT n FROM News n ORDER BY n.publishTime DESC")
    List<News> findAllByOrderByPublishTimeDesc(Pageable pageable);

    // 按分类的最新新闻
    @Query("SELECT n FROM News n WHERE n.category = :category ORDER BY n.publishTime DESC")
    List<News> findByCategoryOrderByPublishTimeDesc(@Param("category") String category, Pageable pageable);

    // 随机新闻
    @Query(value = "SELECT * FROM news ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<News> findRandomNews(@Param("limit") int limit);

    // 时间段内的新闻
    @Query("SELECT n FROM News n WHERE n.publishTime >= :startTime AND n.publishTime <= :endTime ORDER BY n.publishTime DESC")
    List<News> findByPublishTimeBetween(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        Pageable pageable);

    // 获取多个分类的新闻
    @Query("SELECT n FROM News n WHERE n.category IN :categories ORDER BY n.publishTime DESC")
    List<News> findByCategories(@Param("categories") List<String> categories, Pageable pageable);

    @Query("SELECT n FROM News n ORDER BY n.viewCount DESC")
    List<News> findTopByOrderByViewCountDesc(PageRequest of);


    @Query("""
        SELECT n, COUNT(ub.id) as clickCount 
        FROM News n 
        LEFT JOIN UserBehavior ub ON n.newsId = ub.newsId AND ub.behaviorType = 'view'
        GROUP BY n.newsId 
        ORDER BY clickCount DESC, n.publishTime DESC
        """)
    List<Object[]> findHotNewsByClickCount(Pageable pageable);

    // 或者使用原生SQL（如果性能需要）
    @Query(value = """
        SELECT n.*, COUNT(ub.id) as click_count 
        FROM news n 
        LEFT JOIN user_behavior ub ON n.news_id = ub.news_id AND ub.behavior_type = 'view'
        GROUP BY n.news_id 
        ORDER BY click_count DESC, n.publish_time DESC 
        LIMIT :limit
        """, nativeQuery = true)
    List<News> findHotNewsNative(@Param("limit") int limit);


    @Query("SELECT n FROM News n WHERE n.hasImages = true ORDER BY n.publishTime DESC")
    List<News> findNewsWithImages(Pageable pageable);

    // 按分类查询有图片的新闻
    @Query("SELECT n FROM News n WHERE n.category = :category AND n.hasImages = true ORDER BY n.publishTime DESC")
    List<News> findNewsWithImagesByCategory(@Param("category") String category, Pageable pageable);

    // 查询热门图片新闻（按热度）
    @Query("""
        SELECT n FROM News n 
        WHERE n.hasImages = true 
        ORDER BY n.heatScore DESC, n.publishTime DESC
        """)
    List<News> findHotNewsWithImages(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.category = :category AND n.newsId != :excludeNewsId ORDER BY n.publishTime DESC")
    List<News> findByCategoryExcludingId(
            @Param("category") String category,
            @Param("excludeNewsId") String excludeNewsId,
            Pageable pageable);


    // 根据分类查找新闻
    List<News> findByCategory(String category);

    // 查找热门新闻
    List<News> findByIsHotTrueOrderByHeatScoreDesc();

    // 查找最近发布的新闻
    List<News> findByPublishTimeAfterOrderByPublishTimeDesc(LocalDateTime time);

    // 自定义查询：增加点赞数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.likeCount = n.likeCount + 1 WHERE n.newsId = :newsId")
    int incrementLikeCount(@Param("newsId") String newsId);

    // 自定义查询：减少点赞数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.likeCount = n.likeCount - 1 WHERE n.newsId = :newsId AND n.likeCount > 0")
    int decrementLikeCount(@Param("newsId") String newsId);

    // 自定义查询：增加收藏数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.collectCount = n.collectCount + 1 WHERE n.newsId = :newsId")
    int incrementCollectCount(@Param("newsId") String newsId);

    // 自定义查询：减少收藏数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.collectCount = n.collectCount - 1 WHERE n.newsId = :newsId AND n.collectCount > 0")
    int decrementCollectCount(@Param("newsId") String newsId);

    // 自定义查询：增加浏览数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.viewCount = n.viewCount + 1 WHERE n.newsId = :newsId")
    int incrementViewCount(@Param("newsId") String newsId);

    // 批量增加浏览数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.viewCount = n.viewCount + :increment WHERE n.newsId IN :newsIds")
    int batchIncrementViewCount(
            @Param("newsIds") List<String> newsIds,
            @Param("increment") int increment
    );

    // 更新热度分数
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.heatScore = :heatScore WHERE n.newsId = :newsId")
    int updateHeatScore(@Param("newsId") String newsId, @Param("heatScore") Double heatScore);

    // 设置热门状态
    @Modifying
    @Transactional
    @Query("UPDATE News n SET n.isHot = :isHot WHERE n.newsId = :newsId")
    int updateHotStatus(@Param("newsId") String newsId, @Param("isHot") Boolean isHot);

    // 查找浏览量最高的新闻
    @Query("SELECT n FROM News n ORDER BY n.viewCount DESC LIMIT :limit")
    List<News> findTopByViewCount(@Param("limit") int limit);

    // 查找点赞数最高的新闻
    @Query("SELECT n FROM News n ORDER BY n.likeCount DESC LIMIT :limit")
    List<News> findTopByLikeCount(@Param("limit") int limit);

    // 根据热度分数排序
    List<News> findAllByOrderByHeatScoreDesc();
}

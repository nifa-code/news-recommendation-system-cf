package com.fanninews.recommender.repository;

import com.fanninews.recommender.entity.UserBehavior;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserBehaviorRepository extends JpaRepository<UserBehavior, Long> {
    // 获取用户已读新闻ID
    @Query("SELECT DISTINCT ub.newsId FROM UserBehavior ub WHERE ub.userId = :userId AND ub.behaviorType = 'VIEW'")
    Set<String> findViewedNewsIdsByUserId(@Param("userId") String userId);

    // 统计新闻热度（基于用户行为）
    @Query("SELECT ub.newsId, COUNT(ub) as interactionCount FROM UserBehavior ub " +
            "WHERE ub.behaviorTime >= :sinceTime " +
            "GROUP BY ub.newsId " +
            "ORDER BY interactionCount DESC")
    List<Object[]> findHotNewsByInteraction(@Param("sinceTime") LocalDateTime sinceTime, Pageable pageable);

    // 统计用户行为数量
    @Query("SELECT COUNT(ub) FROM UserBehavior ub WHERE ub.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    // 检查用户是否有某种行为
    boolean existsByUserIdAndNewsIdAndBehaviorType(String userId, String newsId,
                                                   UserBehavior.BehaviorType behaviorType);
    // 获取用户最近行为
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.userId = :userId ORDER BY ub.behaviorTime DESC")
    List<UserBehavior> findRecentBehaviorsByUserId(@Param("userId") String userId, Pageable pageable);

    // 添加时间段查询
    Page<UserBehavior> findByBehaviorTimeAfter(LocalDateTime sinceTime, Pageable pageable);
    @Query("SELECT ub FROM UserBehavior ub WHERE ub.behaviorTime >= :sinceTime")
    List<UserBehavior> findByBehaviorTimeAfter(@Param("sinceTime") LocalDateTime sinceTime);

    // 添加新闻热度统计
    @Query("SELECT ub.newsId, ub.behaviorType, COUNT(ub) as count " +
            "FROM UserBehavior ub " +
            "WHERE ub.behaviorTime >= :sinceTime " +
            "GROUP BY ub.newsId, ub.behaviorType")
    List<Object[]> findNewsBehaviorStats(@Param("sinceTime") LocalDateTime sinceTime);

    @Query("SELECT ub FROM UserBehavior ub WHERE ub.userId = :userId AND ub.behaviorTime >= :sinceTime")
    Page<UserBehavior> findByUserIdAndBehaviorTimeAfter(
            @Param("userId") String userId,
            @Param("sinceTime") LocalDateTime sinceTime,
            Pageable pageable);

    @Query("SELECT ub FROM UserBehavior ub WHERE ub.userId = :userId AND ub.behaviorTime >= :sinceTime")
    List<UserBehavior> findByUserIdAndBehaviorTimeAfter(
            @Param("userId") String userId,
            @Param("sinceTime") LocalDateTime sinceTime);
    // 统计用户对各类新闻的行为次数
    @Query("SELECT n.category, COUNT(ub) FROM UserBehavior ub " +
            "JOIN News n ON ub.newsId = n.newsId " +
            "WHERE ub.userId = :userId AND ub.behaviorTime >= :sinceTime " +
            "GROUP BY n.category")
    List<Object[]> countBehaviorByCategory(
            @Param("userId") String userId,
            @Param("sinceTime") LocalDateTime sinceTime);

    @Query("SELECT COUNT(DISTINCT ub.userId) FROM UserBehavior ub")
    long countDistinctUsers();


    Optional<UserBehavior> findByUserIdAndNewsIdAndBehaviorType(
            String userId,
            String newsId,
            UserBehavior.BehaviorType behaviorType
    );


    List<UserBehavior> findByUserIdAndBehaviorType(String userId, UserBehavior.BehaviorType behaviorType);

    @Query("SELECT COUNT(ub) FROM UserBehavior ub WHERE ub.userId = :userId AND ub.behaviorType = 'LIKE'")
    Integer countLikesByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(ub) FROM UserBehavior ub WHERE ub.userId = :userId AND ub.behaviorType = 'COLLECT'")
    Integer countCollectsByUserId(@Param("userId") String userId);

    // ========== 新增：浏览历史/收藏列表所需方法（无爆红） ==========
    /**
     * 查询用户指定行为的记录（按时间倒序）
     * 用途：获取浏览历史（VIEW）/收藏列表（COLLECT）
     */
    List<UserBehavior> findByUserIdAndBehaviorTypeOrderByBehaviorTimeDesc(
            String userId,
            UserBehavior.BehaviorType behaviorType
    );

    /**
     * 分页查询用户指定行为的记录（按时间倒序）
     * 用途：浏览历史/收藏列表的分页展示
     */
    List<UserBehavior> findByUserIdAndBehaviorTypeOrderByBehaviorTimeDesc(
            String userId,
            UserBehavior.BehaviorType behaviorType,
            Pageable pageable
    );

    /**
     * 统计用户指定行为的记录总数
     * 用途：分页时计算总页数
     */
    long countByUserIdAndBehaviorType(
            String userId,
            UserBehavior.BehaviorType behaviorType
    );

    /**
     * 联表查询：用户行为 + 新闻详情（高效获取浏览/收藏列表）
     * 用途：替代Service层遍历查询，提升性能
     */
    @Query("SELECT ub, n FROM UserBehavior ub JOIN News n ON ub.newsId = n.newsId " +
            "WHERE ub.userId = :userId AND ub.behaviorType = :type " +
            "ORDER BY ub.behaviorTime DESC")
    List<Object[]> findUserBehaviorWithNews(
            @Param("userId") String userId,
            @Param("type") UserBehavior.BehaviorType type,
            Pageable pageable
    );


}

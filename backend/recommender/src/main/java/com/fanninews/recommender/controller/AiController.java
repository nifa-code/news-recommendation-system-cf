package com.fanninews.recommender.controller;

import com.fanninews.recommender.dto.request.QnaRequest;
import com.fanninews.recommender.entity.LimitType;
import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.service.DeepseekService;
import com.fanninews.recommender.service.producer.AITaskProducer;
import com.fanninews.recommender.utils.RateLimiter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.DigestUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AiController {
    private final DeepseekService aiService;
    private final NewsRepository newsRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AITaskProducer aiTaskProducer;
    @PostMapping("/summary")
    @RateLimiter(key = "ai:summary:{#userId}", time = 3600, count = 10, limitType = LimitType.USER)
    public ResponseEntity<Map<String,String>> generateSummary(@RequestParam @NotBlank @Size(min = 1) String newsId,
                                                              @AuthenticationPrincipal UserDetails user){
        String cacheKey="ai:summary:"+newsId;
        String cached=redisTemplate.opsForValue().get(cacheKey);
        if(cached!=null) {
            return ResponseEntity.ok(Map.of("summary", cached, "cached", "true"));
        }
        aiTaskProducer.sendAITask("SUMMARY", newsId, null,cacheKey);
        return ResponseEntity.accepted()
                .body(Map.of("message", "摘要生成中，请稍后查询", "taskId", cacheKey));

    }
    @GetMapping("/summary/result")
    @RateLimiter(key = "ai:summary:{#userId}", time = 3600, count = 10, limitType = LimitType.USER)
    public ResponseEntity<?> getSummaryResult(@RequestParam String taskId){
        String result=redisTemplate.opsForValue().get(taskId);
        if(result==null){
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "processing"));
        }
        return ResponseEntity.ok(Map.of("summary",result));
    }

    @PostMapping("qna")
    @RateLimiter(key = "ai:qna:{#userId}", time = 3600, count = 10, limitType = LimitType.USER)
    public ResponseEntity<?> askQuestion(@Valid @RequestBody QnaRequest qnaRequest,
                                         @AuthenticationPrincipal UserDetails user){
        String cacheKey = "ai:qna:" + qnaRequest.getNewsId() + ":" +
                DigestUtils.md5DigestAsHex(qnaRequest.getQuestion().getBytes()); // 用ID和问题生成唯一缓存键

        String cachedAnswer = redisTemplate.opsForValue().get(cacheKey);
        if (cachedAnswer != null) {
            return ResponseEntity.ok(Map.of("answer", cachedAnswer, "cached", "true"));
        }
        aiTaskProducer.sendAITask("QNA",qnaRequest.getNewsId(), qnaRequest.getQuestion() ,cacheKey);
        String ownerKey = cacheKey + ":owner";
        redisTemplate.opsForValue().set(ownerKey, user.getUsername(), 13, TimeUnit.HOURS);
        return ResponseEntity.accepted()
                .body(Map.of("message", "问题处理中，请稍后查询结果", "taskId", cacheKey));
    }
    @GetMapping("qna/result")
    @RateLimiter(key="ai:qna:#{userId}", time=3600, count=10, limitType= LimitType.USER)
    public ResponseEntity<?> getQnAResult(@RequestParam String taskId,@AuthenticationPrincipal UserDetails  user){
        //检验权限
        String ownerKey = taskId + ":owner";
        String taskOwner = redisTemplate.opsForValue().get(ownerKey);
        if (taskOwner == null || !taskOwner.equals(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权访问此任务"));
        }

        String result=redisTemplate.opsForValue().get(taskId);
        if(result==null){
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status", "processing"));
        }
        return ResponseEntity.ok(Map.of("answer",result));
    }
}

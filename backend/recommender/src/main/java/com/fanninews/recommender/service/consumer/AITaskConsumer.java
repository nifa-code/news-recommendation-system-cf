package com.fanninews.recommender.service.consumer;

import com.fanninews.recommender.entity.News;
import com.fanninews.recommender.repository.NewsRepository;
import com.fanninews.recommender.service.DeepseekService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel; // 通信通道
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AITaskConsumer {
    @Autowired
    private DeepseekService aiService;
    @Autowired
    private NewsRepository newsRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @RabbitListener(queuesToDeclare = @Queue(name = "ai.task.queue", durable = "true"))
    public void processAiTask(Message message,Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Map<String, String>taskMessage=null;
        try{
            //从message中提取字节并反序列化为Map
            String msgBody=new String(message.getBody(), StandardCharsets.UTF_8);
            taskMessage=objectMapper.readValue(msgBody, new TypeReference<Map<String, String>>() {});
            log.info("接收到消息: {}", taskMessage);

            log.info("接收到消息: {}", taskMessage); // 打印完整消息内容
            String cacheKey = taskMessage.get("cacheKey"); //使用controller传来的缓存键
            String newsId = taskMessage.get("newsId");
            String taskType = taskMessage.get("taskType");
            String question = taskMessage.get("question");

            //业务逻辑
            log.info("开始处理， newsId: {}, cacheKey: {}", newsId, cacheKey);
            News news=newsRepository.findById(newsId).orElseThrow(()->{log.error("新闻不存在，直接拒绝消息。newsId: {}", newsId);return new RuntimeException("新闻不存在");});
            String result;
             if("QNA".equals(taskType)){
                result=aiService.generateQnA(news.getTitle(),question,news.getAbstractText());
             }else{
                result=aiService.generateSummary(news.getTitle(),news.getAbstractText());
             }
             redisTemplate.opsForValue().set(cacheKey,result,12, TimeUnit.HOURS);
             log.info("任务完成，任务ID: {}, 结果: {}", cacheKey, result);

             //手动确认消息
            channel.basicAck(deliveryTag,false);

        }
        catch (JsonProcessingException e) {
        log.error("消息JSON格式错误，无法解析，消息将被丢弃。消息体: {}", new String(message.getBody(), StandardCharsets.UTF_8), e);
        // 格式错误的消息，拒绝且不重试
        channel.basicNack(deliveryTag, false, false);
        } catch(Exception e){
            log.error("任务处理失败, 失败原因: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, false);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}

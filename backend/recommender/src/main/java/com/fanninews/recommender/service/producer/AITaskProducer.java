package com.fanninews.recommender.service.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AITaskProducer {//生产者
    @Autowired
    private AmqpTemplate rabbitTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public void sendAITask(String taskType,String newsId,String question,String cacheKey){
        try {
            Map<String, String> taskMessage = new HashMap<>();
            taskMessage.put("taskType", taskType);
            taskMessage.put("newsId", newsId);
            taskMessage.put("question", question);
            taskMessage.put("cacheKey", cacheKey);

            String jsonMessage = objectMapper.writeValueAsString(taskMessage);
            MessageProperties props = new MessageProperties();

            props.setContentType("application/json");
            Message amqpMessage = new Message(jsonMessage.getBytes(StandardCharsets.UTF_8), props);

            rabbitTemplate.convertAndSend("ai.task.queue", taskMessage);
            log.info("🚀 发送任务: {}", taskMessage);
            log.info("🚀 推送任务成功");
        }catch(Exception e){
            log.error("🚀 推送任务失败: {}", e.getMessage());
        }

    }





}

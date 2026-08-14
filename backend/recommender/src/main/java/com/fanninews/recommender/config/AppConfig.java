package com.fanninews.recommender.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;
@Configuration
@Slf4j
public class AppConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // 支持LocalDateTime
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        log.info("✅ ObjectMapper  bean 正在被创建...");
        return mapper;
    }
    @Bean
    public String dependencyChecker(
            @Autowired(required = false) RestTemplate rt,
            @Autowired(required = false) ObjectMapper om) {
        log.info("===== 依赖项诊断报告 =====");
        log.info("RestTemplate 是否可用: {}", (rt != null ? "✅ 是" : "❌ 否 - 这将导致注入失败"));
        log.info("ObjectMapper 是否可用: {}", (om != null ? "✅ 是" : "❌ 否"));
        log.info("==========================");
        return "Diagnostics Complete";
    }

}

package com.fanninews.recommender.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "deepseek.api")
@Data  // Lombok 注解，需要添加依赖
public class DeepseekConfig {
    private String key;
    private String baseUrl;
    private String model = "deepseek-chat";  // 默认值
    private int timeout = 30000;  // 默认值
}

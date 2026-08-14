package com.fanninews.recommender.config;

import com.fanninews.recommender.utils.Prase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PraseConfig {
    @Bean
    public Prase prase() {
        return new Prase();
    }
}

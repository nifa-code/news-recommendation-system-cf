package com.fanninews.recommender.utils;

import com.fanninews.recommender.entity.LimitType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    String key() default "rate_limit:";
    int time() default 60;
    int count() default 100;
    LimitType limitType() default LimitType.DEFAULT;
    boolean fallback() default true;
}
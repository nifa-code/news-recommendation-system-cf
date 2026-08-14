package com.fanninews.recommender.entity;

public enum LimitType {
    DEFAULT,
    IP,
    USER,       // 按用户限流
    IP_AND_USER
}
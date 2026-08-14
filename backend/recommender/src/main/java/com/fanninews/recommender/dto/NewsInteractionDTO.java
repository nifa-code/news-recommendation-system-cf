package com.fanninews.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewsInteractionDTO {
    private String userId;
    private String newsId;
    private double rating;
}
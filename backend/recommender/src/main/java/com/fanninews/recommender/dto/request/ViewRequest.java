package com.fanninews.recommender.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public  class ViewRequest {
    @NotBlank
    private String newsId;
    private Integer durationSeconds;
}

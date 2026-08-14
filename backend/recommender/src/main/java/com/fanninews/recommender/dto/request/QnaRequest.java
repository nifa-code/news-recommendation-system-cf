package com.fanninews.recommender.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QnaRequest {
    @NotNull
    private String newsId;
    @NotBlank
    @Size(max = 200) private String question;
}

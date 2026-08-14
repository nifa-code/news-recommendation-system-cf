package com.fanninews.recommender.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchNewsRequest {
    @NotNull
    @Size(min = 1, max = 100)
    private List<String> newsIds;
}
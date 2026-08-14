package com.fanninews.recommender.dto.request;

import lombok.Data;

@Data
public class PythonApiRequest {
    private String user_id;
    private Integer top_k;
    private Boolean use_diversity=true;
}

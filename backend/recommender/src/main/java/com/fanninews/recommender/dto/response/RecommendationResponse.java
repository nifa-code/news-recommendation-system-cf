package com.fanninews.recommender.dto.response;
import com.fanninews.recommender.dto.NewsDTO;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendationResponse {
    private String userId;
    private List<NewsDTO> recommendations;
    private String algorithm; // CF:协同过滤, HOT:热门
    private Boolean isColdStart;
    private String message;
    private LocalDateTime generatedAt;
    public RecommendationResponse() {
        this.generatedAt = LocalDateTime.now();
    }
}
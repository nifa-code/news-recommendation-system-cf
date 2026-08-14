package com.fanninews.recommender.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String username;
    private String email;
    private String nickname;
    private String avatarUrl;
}
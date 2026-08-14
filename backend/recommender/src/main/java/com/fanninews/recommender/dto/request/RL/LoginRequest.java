package com.fanninews.recommender.dto.request.RL;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String userId;

    @NotBlank(message = "密码不能为空")
    private String password;
}

package com.fanninews.recommender.service;
import com.fanninews.recommender.config.DeepseekConfig;
import com.fanninews.recommender.dto.request.*;
import com.fanninews.recommender.dto.response.DeepSeekResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DeepseekService {
    private final DeepseekConfig deepseekConfig;
    private OkHttpClient client;
    private ObjectMapper objectMapper;
    private String chatEndpoint;


    public DeepseekService(DeepseekConfig deepseekConfig) {
        this.deepseekConfig = deepseekConfig;
    }

    @PostConstruct
    public void init(){
        try {
            String apiKey = deepseekConfig.getKey();
            String apiBaseUrl = deepseekConfig.getBaseUrl();
            String model = deepseekConfig.getModel();
            int timeout = deepseekConfig.getTimeout();
            log.info("开始初始化 DeepseekService...");
            log.info("API Key: {}", apiKey != null ? "已设置（长度：" + apiKey.length() + "）" : "未设置");
            log.info("API Base URL: {}", apiBaseUrl);
            log.info("Model: {}", model);
            log.info("Timeout: {}", timeout);

            // 1. 校验核心配置
            if (apiBaseUrl == null || apiBaseUrl.trim().isEmpty()) {
                throw new IllegalStateException("deepseek.api.base-url 未配置！");
            }
            if (model == null || model.trim().isEmpty()) {
                throw new IllegalStateException("deepseek.api.model 未配置！");
            }

            // 2. 修复 URL 拼接（避免末尾无 / 导致拼接错误）
            String finalBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl : apiBaseUrl + "/";
            this.chatEndpoint = finalBaseUrl + "chat/completions";
            log.info("拼接后的 Chat 端点: {}", chatEndpoint);

            // 3. 初始化 ObjectMapper
            this.objectMapper = new ObjectMapper();
            this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // 4. 初始化 OkHttpClient（校验 timeout 合法性）
            if (timeout <= 0) {
                throw new IllegalStateException("deepseek.api.timeout 配置非法：" + timeout + "（必须大于0）");
            }
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                    .readTimeout(timeout, TimeUnit.MILLISECONDS)
                    .build();

            log.info("DeepSeekService 初始化完成，模型: {}, 端点: {}", model, chatEndpoint);
        } catch (Exception e) {
            log.error("DeepSeekService 初始化失败！", e); // 打印完整异常栈
            throw new RuntimeException("DeepSeekService 初始化失败", e); // 强制抛出，让 Spring 感知
        }
    }


//    @PostConstruct
//    public void init(){
//        log.info("API Key: {}", apiKey != null ? "已设置" : "未设置");
//        log.info("API Base URL: {}", apiBaseUrl);
//        log.info("Model: {}", model);
//        if(apiKey==null||apiKey.trim().isEmpty()){
//            log.error("DeepSeek API Key 未配置！请在环境变量或配置文件中设置 DEEPSEEK_API_KEY。");
//        }
//        this.objectMapper = new ObjectMapper();
//        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);//忽略JSON里多余/未定义的字段
//        this.chatEndpoint = apiBaseUrl + "/chat/completions";
//
//        //初始化Http客户端
//        this.client=new OkHttpClient.Builder()
//                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
//                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
//                .readTimeout(timeout, TimeUnit.MILLISECONDS)
//                .build();
//
//        log.info("DeepSeekService 初始化完成，模型: {}, 端点: {}", model, chatEndpoint);
//
//    }
    public String generateAnswer(String prompt) throws IOException {
        List<DeepSeekRequest.Message> messages= List.of(new  DeepSeekRequest.Message("user",prompt) );

        DeepSeekRequest drequest=new DeepSeekRequest(deepseekConfig.getModel(), messages);

        String jsonRequestBody=objectMapper.writeValueAsString(drequest);
        Request request=new Request.Builder()
                .url(chatEndpoint)
                .post(RequestBody.create(jsonRequestBody, MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + deepseekConfig.getKey())
                .addHeader("Content-type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response=client.newCall(request).execute()){
                if(!response.isSuccessful()){
                    String errorBody=response.body()!=null?response.body().string():null;
                    log.error("DeepSeek API 请求失败。状态码: {}, 响应体: {}", response.code(), errorBody);
                    throw new IOException("API request failed with code: " + response.code() + ", body: " + errorBody);
                }
                String responseBody=response.body().string();
                DeepSeekResponse apiResponse=objectMapper.readValue(responseBody, DeepSeekResponse.class);
                if(apiResponse.getChoices()!=null&&!apiResponse.getChoices().isEmpty()){
                    String content=apiResponse.getChoices().get(0).getMessage().getContent();
                    log.debug("DeepSeek调用成功，消耗Token数: {}",
                            apiResponse.getUsage() != null ? apiResponse.getUsage().getTotal_tokens() : "未知");
                    return content.trim();
                } else {
                    throw new IOException("API响应中未包含有效结果。完整响应: " + responseBody);
                }
            } catch (SocketTimeoutException e) {
            log.error("调用DeepSeek API超时，请检查网络或增加超时设置。", e);
            throw new IOException("请求超时，请稍后重试");
        }
    }

    public String generateSummary(String title,String content) throws IOException{
        String prompt = String.format("""
        你是一个资深的新闻编辑。请为以下新闻生成一段**核心摘要**，要求：
        1. 严格控制在80字以内。
        2. 提炼出**谁**、**做了什么**、**关键结果或影响**。
        3. 语言精炼、客观，直接陈述事实。

        标题：《%s》
        正文：%s
        """, title, content.length() > 1500 ? content.substring(0, 1500) + "..." : content);

       return generateAnswer(prompt);
    }

    public String generateQnA(String title,String userQuestion,String content) throws IOException{
        String prompt = String.format("""
        请严格根据以下新闻内容，以朋友般亲切的口吻回答用户的问题。
        如果问题与新闻完全无关,也给出专业严谨的回答。
        
        【新闻标题】%s
        【新闻内容】%s
        ---
        【用户问题】%s
        ---
        【你的回答】：
        """, title,
                content.length() > 1800 ? content.substring(0, 1800) + "..." : content,
                userQuestion);
        return generateAnswer(prompt);
    }

}

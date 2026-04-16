package com.example.ccc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bailian")
public class BailianProperties {
    /**
     * OpenAI-compatible base url, e.g. https://dashscope.aliyuncs.com/compatible-mode/v1
     */
    private String baseUrl;
    private String apiKey;
    private String model;
}


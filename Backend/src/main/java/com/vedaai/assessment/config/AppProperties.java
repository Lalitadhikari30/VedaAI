package com.vedaai.assessment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {
    private String geminiApiKey;
    private String geminiModel = "gemini-3.6-flash";
    private int maxPages = 30;
    private String frontendUrl;
    private int sessionTtlMinutes = 30;
}

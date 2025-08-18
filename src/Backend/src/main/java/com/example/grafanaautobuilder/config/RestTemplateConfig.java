package com.example.grafanaautobuilder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// Spring's traditional HTTP client for making REST API calls
// Used to communicate with other services/APIs
// Will be deprecated in future Spring versions in favor of WebClient
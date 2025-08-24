package com.example.grafanaautobuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GrafanaAutobuilderApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrafanaAutobuilderApplication.class, args);
    }
} 
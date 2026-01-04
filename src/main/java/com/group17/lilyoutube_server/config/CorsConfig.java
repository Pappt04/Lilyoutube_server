package com.group17.lilyoutube_server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                //TODO Change after testing
                registry.addMapping("/**")
                        .allowedOriginPatterns("*") // Allows any origin even with credentials
                        .allowedMethods("*")        // Allows all HTTP methods
                        .allowedHeaders("*")        // Allows all headers
                        .allowCredentials(true)     // Allows cookies/auth headers
                        .maxAge(3600);
            }
        };
    }
}
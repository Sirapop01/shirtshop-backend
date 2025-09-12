package com.shirtshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // กำหนดให้ใช้กับทุก path ที่ขึ้นต้นด้วย /api/
                .allowedOrigins("http://localhost:3000") // อนุญาตเฉพาะ origin นี้
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // อนุญาต HTTP methods เหล่านี้
                .allowedHeaders("*") // อนุญาตทุก headers
                .allowCredentials(true); // อนุญาตการส่ง credentials (เช่น cookies)
    }
}
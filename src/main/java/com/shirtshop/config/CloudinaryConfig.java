package com.shirtshop.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud_name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api_key:${CLOUDINARY_API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api_secret:${CLOUDINARY_API_SECRET:}}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        validate("cloudinary.cloud_name/CLOUDINARY_CLOUD_NAME", cloudName);
        validate("cloudinary.api_key/CLOUDINARY_API_KEY", apiKey);
        validate("cloudinary.api_secret/CLOUDINARY_API_SECRET", apiSecret);

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        // Optionally enforce secure URLs:
        config.put("secure", "true");

        return new Cloudinary(config);
    }

    private void validate(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing Cloudinary config: " + key + " is empty. " +
                    "Provide it via application-prod.yml or environment variables.");
        }
    }
}

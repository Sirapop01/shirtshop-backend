package com.shirtshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // ไว้ขยาย config เพิ่มได้ เช่น converters ฯลฯ
}

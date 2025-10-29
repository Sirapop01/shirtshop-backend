// src/main/java/com/shirtshop/ShirtShopBackendApplication.java
package com.shirtshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.shirtshop")  // ✅ บังคับสแกน controller/service/repo ทั้งหมด
@EnableScheduling
public class ShirtShopBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShirtShopBackendApplication.class, args);
    }
}

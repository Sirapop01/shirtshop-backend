// src/main/java/com/shirtshop/entity/PaymentSettings.java
package com.shirtshop.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Document(collection = "settings")
public class PaymentSettings {
    @Id
    private String id = "PAYMENT_PROMPTPAY"; // fixed key

    private String target;          // เบอร์/พร้อมเพย์ไอดี
    private int expireMinutes;      // นาทีที่ QR จะหมดอายุ
    private boolean enabled = true;

    private Instant updatedAt = Instant.now();
}

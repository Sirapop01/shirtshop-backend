// src/main/java/com/shirtshop/entity/Order.java
package com.shirtshop.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    private String userId;
    private List<OrderItem> items = new ArrayList<>();
    private int subTotal;
    private int shippingFee;
    private int total;

    private PaymentMethod paymentMethod;   // PROMPTPAY
    private OrderStatus status;            // ดู enum ด้านบน

    // PromptPay info (เพื่อแสดง QR)
    private String promptpayTarget;        // หมายเลข PromptPay/เบอร์/เลขบัตรประชาชน (string)
    private String promptpayQrUrl;         // URL รูปจาก promptpay.io

    // Slip
    private String paymentSlipUrl;         // URL สลิป (Cloudinary)
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant expiresAt;
}

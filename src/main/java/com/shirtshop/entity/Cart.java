// src/main/java/com/shirtshop/entity/Cart.java
package com.shirtshop.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "carts")
public class Cart {
    @Id
    private String id;
    private String userId;            // ✅ เก็บเป็น String เสมอ
    private List<CartItem> items = new ArrayList<>();
    private int subTotal;             // คิดรวมฝั่ง BE ส่งกลับให้ FE
    private int shippingFee;          // ถ้าไม่ใช้ คงไว้เป็น 0
    private Instant createdAt;
    private Instant updatedAt;
}

// src/main/java/com/shirtshop/entity/Cart.java
package com.shirtshop.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "carts")
public class Cart {
    @Id
    private String id;
    private String userId; // อ้างอิงผู้ใช้ที่ล็อกอิน

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private Integer shippingFee;
    private Integer subTotal;
    private Instant createdAt;
    private Instant updatedAt;
}

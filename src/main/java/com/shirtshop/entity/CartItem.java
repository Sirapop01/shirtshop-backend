// src/main/java/com/shirtshop/entity/CartItem.java
package com.shirtshop.entity;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    private String productId;
    private String color;
    private String size;
    private int quantity;

    // snapshot เบื้องต้นเพื่อโชว์ใน cart (อัปเดตใหม่ตอนเช็คเอาต์)
    private String productName;
    private String imageUrl;
    private double unitPrice; // ถ้าราคาในระบบเป็น BigDecimal ก็ใช้ BigDecimal ได้
}

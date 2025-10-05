// src/main/java/com/shirtshop/entity/CartItem.java
package com.shirtshop.entity;

import lombok.Data;

@Data
public class CartItem {
    private String productId;
    private String name;      // optional (FE ไม่ได้ใช้ขณะบันทึก)
    private String imageUrl;  // optional
    private int    unitPrice; // เก็บเป็นจำนวนเต็มสตางค์หรือบาทก็ได้ แต่ FE map จาก unitPrice/price
    private String color;
    private String size;
    private int    quantity;
}

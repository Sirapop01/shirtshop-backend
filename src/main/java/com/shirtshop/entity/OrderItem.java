// src/main/java/com/shirtshop/entity/OrderItem.java
package com.shirtshop.entity;

import lombok.Data;

@Data
public class OrderItem {
    private String productId;
    private String name;
    private String imageUrl;
    private int unitPrice;
    private String color;
    private String size;
    private int quantity;
}

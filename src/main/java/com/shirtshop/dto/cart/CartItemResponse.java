// src/main/java/com/shirtshop/dto/cart/CartItemResponse.java
package com.shirtshop.dto.cart;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemResponse {
    private String productId;
    private String name;
    private String imageUrl;
    private double unitPrice;
    private String color;
    private String size;
    private int quantity;
    private double lineTotal;
}

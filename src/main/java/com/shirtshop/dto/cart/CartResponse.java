// src/main/java/com/shirtshop/dto/cart/CartResponse.java
package com.shirtshop.dto.cart;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    private List<CartItemResponse> items;
    private double subTotal;
}

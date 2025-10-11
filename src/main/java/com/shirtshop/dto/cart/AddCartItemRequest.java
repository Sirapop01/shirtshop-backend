// src/main/java/com/shirtshop/dto/cart/AddCartItemRequest.java
package com.shirtshop.dto.cart;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddCartItemRequest {
    private String productId;
    private String color;
    private String size;
    private int quantity;
}

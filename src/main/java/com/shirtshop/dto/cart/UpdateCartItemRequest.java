// src/main/java/com/shirtshop/dto/cart/UpdateCartItemRequest.java
package com.shirtshop.dto.cart;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateCartItemRequest {
    private String productId;
    private String color;
    private String size;
    private int quantity; // ค่าใหม่
}

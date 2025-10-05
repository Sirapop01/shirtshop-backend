// src/main/java/com/shirtshop/dto/UpdateCartItemRequest.java
package com.shirtshop.dto.cart;

public record UpdateCartItemRequest(
        String productId,
        String color,
        String size,
        int quantity
) {}

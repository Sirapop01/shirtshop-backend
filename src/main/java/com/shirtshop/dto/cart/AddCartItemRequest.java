// src/main/java/com/shirtshop/dto/AddCartItemRequest.java
package com.shirtshop.dto.cart;

public record AddCartItemRequest(
        String productId,
        String color,
        String size,
        int quantity
) {}

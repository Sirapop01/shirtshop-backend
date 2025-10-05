// src/main/java/com/shirtshop/dto/CreateOrderRequest.java
package com.shirtshop.dto;
public record CreateOrderRequest(
        String paymentMethod // "PROMPTPAY"
) {}

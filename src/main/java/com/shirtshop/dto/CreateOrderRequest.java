// src/main/java/com/shirtshop/dto/CreateOrderRequest.java
package com.shirtshop.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String paymentMethod, // "PROMPTPAY"
        @NotBlank String addressId      // ไอดีที่อยู่จัดส่งของผู้ใช้
) {}

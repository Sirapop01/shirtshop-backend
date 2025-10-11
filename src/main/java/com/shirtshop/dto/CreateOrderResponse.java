// src/main/java/com/shirtshop/dto/CreateOrderResponse.java
package com.shirtshop.dto;

import java.time.Instant;

public record CreateOrderResponse(
        String orderId,
        int total,
        String promptpayTarget,
        String promptpayQrUrl,
        Instant expiresAt
) {}

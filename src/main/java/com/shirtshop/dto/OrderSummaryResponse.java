// src/main/java/com/shirtshop/dto/OrderSummaryResponse.java
package com.shirtshop.dto;

import com.shirtshop.entity.OrderStatus;
import com.shirtshop.entity.PaymentMethod;

import java.time.Instant;

public record OrderSummaryResponse(
        String id,
        Instant createdAt,
        Instant expiresAt,
        int total,
        int itemsCount,
        PaymentMethod paymentMethod,
        OrderStatus status,
        String paymentSlipUrl
) {}

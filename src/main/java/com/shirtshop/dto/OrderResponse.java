// src/main/java/com/shirtshop/dto/OrderResponse.java
package com.shirtshop.dto;

import com.shirtshop.entity.OrderStatus;
import com.shirtshop.entity.PaymentMethod;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OrderResponse(
        String id,
        String userId,
        List<Map<String,Object>> items,
        int subTotal,
        int shippingFee,
        int total,
        PaymentMethod paymentMethod,
        OrderStatus status,
        String promptpayTarget,
        String promptpayQrUrl,
        Instant expiresAt,
        String paymentSlipUrl,
        Instant createdAt,
        Instant updatedAt
) {}

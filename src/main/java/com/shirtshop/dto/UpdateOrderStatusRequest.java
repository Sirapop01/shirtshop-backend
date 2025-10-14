package com.shirtshop.dto;

import com.shirtshop.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,  // PAID หรือ REJECTED
        String rejectReason           // ใส่เมื่อ REJECTED
) {}

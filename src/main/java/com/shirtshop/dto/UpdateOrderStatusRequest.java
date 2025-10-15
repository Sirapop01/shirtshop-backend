// src/main/java/com/shirtshop/dto/UpdateOrderStatusRequest.java
package com.shirtshop.dto;

import com.shirtshop.entity.OrderStatus;

public record UpdateOrderStatusRequest(
        OrderStatus status,     // "PAID" | "REJECTED" | "CANCELED"
        String rejectReason     // ใช้ได้ทั้ง reject/cancel (optional)
) {}

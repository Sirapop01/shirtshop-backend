// src/main/java/com/shirtshop/dto/UpdateOrderStatusRequest.java
package com.shirtshop.dto;

import com.shirtshop.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

/**
 * ใช้ PATCH /api/admin/orders/{id}/status
 * - status: PAID | REJECTED | CANCELED (required)
 * - note: เหตุผล (optional) — รับได้ทั้ง key "note" และ "rejectReason"
 */
public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        @JsonAlias({"rejectReason"}) String note
) {}
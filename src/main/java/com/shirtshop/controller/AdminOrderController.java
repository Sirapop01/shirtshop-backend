package com.shirtshop.controller;

import com.shirtshop.dto.OrderListResponse;
import com.shirtshop.dto.OrderResponse;
import com.shirtshop.dto.UpdateOrderStatusRequest;
import com.shirtshop.entity.OrderStatus;
import com.shirtshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * List orders for admin (supports keyword/status filters + pagination)
     * GET /api/admin/orders?page=0&size=10&status=SLIP_UPLOADED&keyword=abc
     */
    @GetMapping
    public OrderListResponse list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, name = "status") List<OrderStatus> statuses,
            Pageable pageable
    ) {
        return orderService.adminList(keyword, statuses, pageable);
    }

    /** Get single order by id */
    @GetMapping("/{orderId}")
    public OrderResponse getOne(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    /** Change order status (admin) */
    @PatchMapping(value = "/{orderId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponse changeStatus(@PathVariable String orderId,
                                      @RequestBody @Valid UpdateOrderStatusRequest req) {
        String adminId = "ADMIN"; // TODO: pull from SecurityContext
        return orderService.adminChangeStatus(orderId, req.status(), adminId, req.note());
    }
}

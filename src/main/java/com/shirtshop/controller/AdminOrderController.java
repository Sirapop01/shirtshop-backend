package com.shirtshop.controller;

import com.shirtshop.dto.UpdateOrderStatusRequest;
import com.shirtshop.dto.OrderResponse;           // ถ้ามี DTO นี้อยู่แล้ว
import com.shirtshop.entity.Order;
import com.shirtshop.entity.OrderStatus;
import com.shirtshop.repository.OrderRepository;
import com.shirtshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderRepository orderRepo;
    private final OrderService orderService;

    // ตาราง Check the Order
    @GetMapping
    public Page<Order> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return (status == null)
                ? orderRepo.findAll(pageable)
                : orderRepo.findAllByStatus(status, pageable);
    }

    // ✅ ใช้กับปุ่ม View
    @GetMapping("/{orderId}")
    public OrderResponse getOne(@PathVariable String orderId) {
        return orderService.getOrder(orderId);    // คืนเป็น DTO ที่หน้าเว็บอ่านได้
    }

    // ✅ ใช้กับปุ่ม Approve / Reject / Cancel
    @PatchMapping(value = "/{orderId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderResponse changeStatus(@PathVariable String orderId,
                                      @RequestBody @Valid UpdateOrderStatusRequest req) {
        // ดึง adminId จาก token ก็ได้; ตอนนี้ส่ง "ADMIN" ไปก่อน
        return orderService.adminChangeStatus(orderId, req.status().name(), "ADMIN", req.rejectReason());

    }
}

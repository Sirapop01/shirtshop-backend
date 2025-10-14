package com.shirtshop.controller;

import com.shirtshop.dto.UpdateOrderStatusRequest;
import com.shirtshop.entity.Order;
import com.shirtshop.entity.OrderStatus;
import com.shirtshop.repository.OrderRepository;
import com.shirtshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ต้องเป็นแอดมินเท่านั้น
public class AdminOrderController {

    private final OrderRepository orderRepo;
    private final OrderService orderService;

    // ✅ ดึงรายการออเดอร์ทั้งหมด หรือกรองตามสถานะ
    @GetMapping
    public Page<Order> list(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status == null) return orderRepo.findAll(pageable);
        return orderRepo.findAllByStatus(status, pageable);
    }

    // ✅ แอดมินเปลี่ยนสถานะออเดอร์
    @PatchMapping("/{orderId}/status")
    public Order changeStatus(@PathVariable String orderId,
                              @RequestBody @Valid UpdateOrderStatusRequest req) {
        // (ดึง adminId จาก token ถ้ามีระบบ security ของคุณ)
        return orderService.adminChangeStatus(orderId, req.status(), "ADMIN", req.rejectReason());
    }
}

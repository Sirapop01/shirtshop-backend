// src/main/java/com/shirtshop/controller/OrderController.java
package com.shirtshop.controller;

import com.shirtshop.dto.CreateOrderRequest;
import com.shirtshop.dto.CreateOrderResponse;
import com.shirtshop.dto.OrderListResponse;
import com.shirtshop.dto.OrderResponse;
import com.shirtshop.entity.OrderStatus;
import com.shirtshop.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final OrderService orderService;

    /** สร้างออเดอร์ (PromptPay) */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createPromptPayOrder(req));
    }


    /** ดึงรายละเอียดออเดอร์ตาม id (รับเฉพาะ 24 ตัวอักษร hex) */
    @GetMapping("/{id:[a-f0-9]{24}}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    /** รายการออเดอร์ของผู้ใช้ปัจจุบัน (มีกรองสถานะ + เพจได้) */
    @GetMapping("/my")
    public ResponseEntity<OrderListResponse> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "status", required = false) java.util.List<OrderStatus> statuses
    ) {
        return ResponseEntity.ok(orderService.listMyOrders(statuses, page, size));
    }

    /** อัปสลิปแนบการโอน */
    @PostMapping("/{id:[a-f0-9]{24}}/slip")
    public ResponseEntity<OrderResponse> uploadSlip(
            @PathVariable String id,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) {
        return ResponseEntity.ok(orderService.uploadSlip(id, file));
    }
}

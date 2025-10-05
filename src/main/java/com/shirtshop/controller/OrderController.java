// src/main/java/com/shirtshop/controller/OrderController.java
package com.shirtshop.controller;

import com.shirtshop.dto.CreateOrderRequest;
import com.shirtshop.dto.CreateOrderResponse;
import com.shirtshop.dto.OrderResponse;
import com.shirtshop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@CrossOrigin
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(orderService.createPromptPayOrder(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PostMapping("/{id}/slip")
    public ResponseEntity<OrderResponse> uploadSlip(@PathVariable String id,
                                                    @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(orderService.uploadSlip(id, file));
    }
}

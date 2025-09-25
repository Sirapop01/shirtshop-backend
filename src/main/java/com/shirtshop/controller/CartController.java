// src/main/java/com/shirtshop/controller/CartController.java
package com.shirtshop.controller;

import com.shirtshop.dto.cart.*;
import com.shirtshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // NOTE: ในตัวอย่างนี้สมมติว่าคุณดึง userId มาจาก SecurityContext หรือ Header
    private String resolveUserId() {
        // TODO: เปลี่ยนเป็นดึงจาก JWT ของคุณ
        return "demo-user-id";
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        return ResponseEntity.ok(cartService.getCart(resolveUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@RequestBody AddCartItemRequest req) {
        return ResponseEntity.ok(cartService.addItem(resolveUserId(), req));
    }

    @PutMapping("/items")
    public ResponseEntity<CartResponse> updateItem(@RequestBody UpdateCartItemRequest req) {
        return ResponseEntity.ok(cartService.updateItem(resolveUserId(), req));
    }

    @DeleteMapping("/items")
    public ResponseEntity<CartResponse> removeItem(
            @RequestParam String productId,
            @RequestParam String color,
            @RequestParam String size) {
        return ResponseEntity.ok(cartService.removeItem(resolveUserId(), productId, color, size));
    }

    @DeleteMapping
    public ResponseEntity<CartResponse> clear() {
        return ResponseEntity.ok(cartService.clear(resolveUserId()));
    }

    @PostMapping("/merge")
    public ResponseEntity<CartResponse> merge(@RequestBody MergeCartRequest req) {
        return ResponseEntity.ok(cartService.merge(resolveUserId(), req));
    }
}

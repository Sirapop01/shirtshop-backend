// src/main/java/com/shirtshop/controller/CartController.java
package com.shirtshop.controller;

import com.shirtshop.dto.cart.AddCartItemRequest;
import com.shirtshop.dto.cart.CartResponse;
import com.shirtshop.dto.cart.UpdateCartItemRequest;
import com.shirtshop.entity.Cart;
import com.shirtshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@CrossOrigin
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getMyCart() {
        Cart c = cartService.getMyCart();
        var items = c.getItems().stream().map(it -> Map.<String,Object>of(
                "productId", it.getProductId(),
                "name", it.getName(),
                "imageUrl", it.getImageUrl(),
                "unitPrice", it.getUnitPrice(),
                "color", it.getColor(),
                "size", it.getSize(),
                "quantity", it.getQuantity()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(new CartResponse(items, c.getSubTotal(), c.getShippingFee()));
    }

    @PostMapping("/items")
    public ResponseEntity<Void> addItem(@RequestBody AddCartItemRequest req) {
        cartService.addItem(req);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/items")
    public ResponseEntity<Void> updateItem(@RequestBody UpdateCartItemRequest req) {
        cartService.updateItem(req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> removeItem(@RequestParam String productId,
                                           @RequestParam String color,
                                           @RequestParam String size) {
        cartService.removeItem(productId, color, size);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        cartService.clearMyCart();
        return ResponseEntity.ok().build();
    }
}

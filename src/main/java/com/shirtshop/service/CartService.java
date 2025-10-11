// src/main/java/com/shirtshop/service/CartService.java
package com.shirtshop.service;

import com.shirtshop.dto.cart.*;
import com.shirtshop.entity.Cart;
import com.shirtshop.entity.CartItem;
import com.shirtshop.entity.Product;
import com.shirtshop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService; // คุณมีอยู่แล้ว

    public Cart getOrCreate(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart c = Cart.builder()
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return cartRepository.save(c);
                });
    }

    public CartResponse getCart(String userId) {
        Cart cart = getOrCreate(userId);
        return toResponse(cart);
    }

    public CartResponse addItem(String userId, AddCartItemRequest req) {
        validateAdd(req);
        Product p = productService.getById(req.getProductId());

        // ตรวจสต็อกจาก variantStocks
        int stock = p.getVariantStocks().stream()
                .filter(v -> equalsIgnoreCaseTrim(v.getColor(), req.getColor())
                        && equalsIgnoreCaseTrim(v.getSize(), req.getSize()))
                .mapToInt(v -> v.getQuantity())
                .sum();

        if (stock <= 0) {
            throw new IllegalArgumentException("Out of stock for selected color/size");
        }

        Cart cart = getOrCreate(userId);

        // upsert
        CartItem exist = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId())
                        && equalsIgnoreCaseTrim(i.getColor(), req.getColor())
                        && equalsIgnoreCaseTrim(i.getSize(), req.getSize()))
                .findFirst().orElse(null);

        int unitPrice = (int) Math.round(p.getPrice().doubleValue()); // ถ้า price เป็น BigDecimal
        String imageUrl = (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) ? p.getImageUrls().get(0) : "";

        if (exist == null) {
            int qty = Math.min(req.getQuantity(), stock);
            cart.getItems().add(CartItem.builder()
                    .productId(p.getId())
                    .color(req.getColor().trim())
                    .size(req.getSize().trim())
                    .quantity(qty)
                    .productName(p.getName())
                    .imageUrl(imageUrl)
                    .unitPrice(unitPrice)
                    .build());
        } else {
            int newQty = exist.getQuantity() + req.getQuantity();
            exist.setQuantity(Math.min(newQty, stock));
            // snapshot อัปเดตชื่อ/รูป/ราคาเผื่อสินค้ามีการแก้ไข
            exist.setProductName(p.getName());
            exist.setImageUrl(imageUrl);
            exist.setUnitPrice(unitPrice);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    public CartResponse updateItem(String userId, UpdateCartItemRequest req) {
        if (req.getQuantity() <= 0) {
            return removeItem(userId, req.getProductId(), req.getColor(), req.getSize());
        }
        Product p = productService.getById(req.getProductId());
        int stock = p.getVariantStocks().stream()
                .filter(v -> equalsIgnoreCaseTrim(v.getColor(), req.getColor())
                        && equalsIgnoreCaseTrim(v.getSize(), req.getSize()))
                .mapToInt(v -> v.getQuantity()).sum();

        if (stock <= 0) throw new IllegalArgumentException("Out of stock");

        Cart cart = getOrCreate(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId())
                        && equalsIgnoreCaseTrim(i.getColor(), req.getColor())
                        && equalsIgnoreCaseTrim(i.getSize(), req.getSize()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not found in cart"));

        item.setQuantity(Math.min(req.getQuantity(), stock));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    public CartResponse removeItem(String userId, String productId, String color, String size) {
        Cart cart = getOrCreate(userId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId)
                && equalsIgnoreCaseTrim(i.getColor(), color)
                && equalsIgnoreCaseTrim(i.getSize(), size));
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    public CartResponse clear(String userId) {
        Cart cart = getOrCreate(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return toResponse(cart);
    }

    public CartResponse merge(String userId, MergeCartRequest req) {
        if (req == null || req.getItems() == null) return getCart(userId);
        for (AddCartItemRequest i : req.getItems()) {
            try {
                addItem(userId, i);
            } catch (Exception ignored) { /* skip item ที่สต็อกไม่พอ */ }
        }
        return getCart(userId);
    }

    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        return (a == null && b == null) ||
                (a != null && b != null && a.trim().equalsIgnoreCase(b.trim()));
    }

    private static void validateAdd(AddCartItemRequest r) {
        if (!StringUtils.hasText(r.getProductId())) throw new IllegalArgumentException("productId required");
        if (!StringUtils.hasText(r.getColor())) throw new IllegalArgumentException("color required");
        if (!StringUtils.hasText(r.getSize())) throw new IllegalArgumentException("size required");
        if (r.getQuantity() <= 0) throw new IllegalArgumentException("quantity must be > 0");
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(i ->
                CartItemResponse.builder()
                        .productId(i.getProductId())
                        .name(i.getProductName())
                        .imageUrl(i.getImageUrl())
                        .unitPrice(i.getUnitPrice())
                        .color(i.getColor())
                        .size(i.getSize())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getUnitPrice() * i.getQuantity())
                        .build()
        ).toList();

        double sub = items.stream().mapToDouble(CartItemResponse::getLineTotal).sum();
        return CartResponse.builder()
                .items(items)
                .subTotal(sub)
                .build();
    }
}

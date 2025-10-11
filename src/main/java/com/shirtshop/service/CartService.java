// src/main/java/com/shirtshop/service/CartService.java
package com.shirtshop.service;

import com.shirtshop.dto.cart.AddCartItemRequest;
import com.shirtshop.dto.cart.UpdateCartItemRequest;
import com.shirtshop.entity.Cart;
import com.shirtshop.entity.CartItem;
import com.shirtshop.entity.User;
import com.shirtshop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService; // ถ้าต้องการ validate/ราคา

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("Unauthenticated");
        Object p = auth.getPrincipal();
        if (p instanceof User u) return u.getId(); // กรณี filter ใส่ User เป็น principal
        if (p instanceof String s) return s;       // กรณี principal เป็น userId
        throw new IllegalStateException("Invalid principal");
    }

    public Cart getMyCartOrCreate() {
        String userId = currentUserId();
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserId(userId);               // ✅ ใส่เป็น String เท่านั้น
                    c.setItems(new ArrayList<>());
                    c.setSubTotal(0);
                    c.setShippingFee(0);
                    c.setCreatedAt(Instant.now());
                    c.setUpdatedAt(Instant.now());
                    return cartRepository.save(c);
                });
    }

    public Cart getMyCart() {
        return cartRepository.findByUserId(currentUserId())
                .orElseGet(this::getMyCartOrCreate);
    }

    public Cart addItem(AddCartItemRequest req) {
        Cart cart = getMyCartOrCreate();

        // ตัวอย่าง: ดึงข้อมูลสินค้าเพื่อ set name/รูป/ราคา (ถ้าต้อง)
        var p = productService.getById(req.productId()); // ต้องมี service นี้อยู่แล้ว
        // หา item เดิม (productId+color+size)
        Optional<CartItem> found = cart.getItems().stream()
                .filter(it -> it.getProductId().equals(req.productId())
                        && it.getColor().equalsIgnoreCase(req.color())
                        && it.getSize().equalsIgnoreCase(req.size()))
                .findFirst();

        if (found.isPresent()) {
            found.get().setQuantity(found.get().getQuantity() + req.quantity());
        } else {
            CartItem it = new CartItem();
            it.setProductId(req.productId());
            it.setName(p.getName());
            it.setImageUrl(p.getImageUrls() != null && !p.getImageUrls().isEmpty() ? p.getImageUrls().get(0) : null);
            it.setUnitPrice(p.getPrice());
            it.setColor(req.color());
            it.setSize(req.size());
            it.setQuantity(req.quantity());
            cart.getItems().add(it);
        }

        recalc(cart);
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart updateItem(UpdateCartItemRequest req) {
        Cart cart = getMyCartOrCreate();
        cart.getItems().stream()
                .filter(it -> it.getProductId().equals(req.productId())
                        && it.getColor().equalsIgnoreCase(req.color())
                        && it.getSize().equalsIgnoreCase(req.size()))
                .findFirst()
                .ifPresent(it -> it.setQuantity(Math.max(0, req.quantity())));

        cart.getItems().removeIf(it -> it.getQuantity() <= 0);
        recalc(cart);
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public Cart removeItem(String productId, String color, String size) {
        Cart cart = getMyCartOrCreate();
        cart.getItems().removeIf(it ->
                it.getProductId().equals(productId)
                        && it.getColor().equalsIgnoreCase(color)
                        && it.getSize().equalsIgnoreCase(size));
        recalc(cart);
        cart.setUpdatedAt(Instant.now());
        return cartRepository.save(cart);
    }

    public void clearMyCart() {
        Cart cart = getMyCartOrCreate();
        cart.getItems().clear();
        recalc(cart);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    private void recalc(Cart cart) {
        int sub = cart.getItems().stream()
                .mapToInt(it -> it.getUnitPrice() * it.getQuantity())
                .sum();
        cart.setSubTotal(sub);
        // shippingFee: กำหนดตามนโยบาย (ที่นี่ใช้ 0)
        cart.setShippingFee(0);
    }
}

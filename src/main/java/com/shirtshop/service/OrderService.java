// src/main/java/com/shirtshop/service/OrderService.java
package com.shirtshop.service;

import com.shirtshop.dto.CreateOrderRequest;
import com.shirtshop.dto.CreateOrderResponse;
import com.shirtshop.dto.OrderResponse;
import com.shirtshop.entity.*;
import com.shirtshop.repository.CartRepository;
import com.shirtshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final ProductService productService;      // ใช้คอนเฟิร์มราคา/ชื่อสินค้า
    private final CloudinaryService cloudinaryService;

    @Value("${app.payment.promptpay.target}") // เช่น "0812345678" หรือ "1234567890123"
    private String promptpayTarget;

    @Value("${app.payment.promptpay.expire-minutes:15}")
    private int expireMinutes;

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = (auth != null ? auth.getPrincipal() : null);
        if (p instanceof User u) return u.getId();
        if (p instanceof String s) return s;
        throw new IllegalStateException("Unauthenticated");
    }

    private String buildPromptPayQrUrl(String target, int totalBaht) {
        // 1) เหลือเฉพาะตัวเลขของ PromptPay (มือถือ/เลขบัตร)
        String digitsOnly = target.replaceAll("\\D", "");

        // 2) amount ต้องเป็น "บาท" และใช้จุด . เป็นทศนิยมเสมอ
        String amountParam = String.format(Locale.US, "%.2f", totalBaht * 1.0);

        // 3) ใส่ timestamp กันรูปเก่าถูก cache
        String ts = String.valueOf(System.currentTimeMillis());

        // 4) URL รูปจาก promptpay.io
        return "https://promptpay.io/"
                + digitsOnly
                + ".png?amount=" + amountParam
                + "&size=360&_ts=" + ts;
    }

    public CreateOrderResponse createPromptPayOrder(CreateOrderRequest req) {
        String userId = currentUserId();

        var cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart not found"));

        // (ทางเลือก) refresh ราคาสินค้าจาก ProductService ทุกครั้ง
        int sub = cart.getItems().stream()
                .mapToInt(it -> it.getUnitPrice() * it.getQuantity())
                .sum();
        int shipping = cart.getShippingFee(); // ถ้าไม่มี = 0
        int total = sub + shipping;
        if (total <= 0) {
            throw new IllegalStateException("Invalid order total: " + total);
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentMethod(PaymentMethod.PROMPTPAY);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        order.setExpiresAt(Instant.now().plus(expireMinutes, ChronoUnit.MINUTES));
        order.setSubTotal(sub);
        order.setShippingFee(shipping);
        order.setTotal(total);

        // map items
        var items = cart.getItems().stream().map(ci -> {
            OrderItem oi = new OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setName(ci.getName());
            oi.setImageUrl(ci.getImageUrl());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setColor(ci.getColor());
            oi.setSize(ci.getSize());
            oi.setQuantity(ci.getQuantity());
            return oi;
        }).collect(Collectors.toList());
        order.setItems(items);

        // สร้าง QR URL
        order.setPromptpayTarget(promptpayTarget);
        String qrDataUrl = PromptPayQr.generatePromptPayQrDataUrl(promptpayTarget, total, 360, true);
        order.setPromptpayQrUrl(qrDataUrl);

        // บันทึก
        order = orderRepo.save(order);

        return new CreateOrderResponse(
                order.getId(),
                order.getTotal(),
                order.getPromptpayTarget(),
                order.getPromptpayQrUrl(),
                order.getExpiresAt()
        );
    }

    public OrderResponse getOrder(String id) {
        var o = orderRepo.findById(id).orElseThrow();
        return toResponse(o);
    }

    public OrderResponse uploadSlip(String orderId, MultipartFile slip) {
        var order = orderRepo.findById(orderId).orElseThrow();

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Order already confirmed");
        }

        // อัปสลิปขึ้น Cloudinary
        var up = cloudinaryService.uploadFile(slip, "shirtshop/slips");
        order.setPaymentSlipUrl(up.getUrl());
        order.setStatus(OrderStatus.SLIP_UPLOADED);
        order.setUpdatedAt(Instant.now());

        order = orderRepo.save(order);
        return toResponse(order);
    }

    // สำหรับ Admin
    public OrderResponse confirm(String id) {
        var o = orderRepo.findById(id).orElseThrow();
        o.setStatus(OrderStatus.PAID);
        o.setPaidAt(Instant.now());
        o.setUpdatedAt(Instant.now());
        orderRepo.save(o);
        return toResponse(o);
    }

    public OrderResponse reject(String id, String reason) {
        var o = orderRepo.findById(id).orElseThrow();
        o.setStatus(OrderStatus.REJECTED);
        o.setUpdatedAt(Instant.now());
        orderRepo.save(o);
        return toResponse(o);
    }

    private OrderResponse toResponse(Order o) {
        var items = o.getItems().stream().map(it -> Map.<String,Object>of(
                "productId", it.getProductId(),
                "name", it.getName(),
                "imageUrl", it.getImageUrl(),
                "unitPrice", it.getUnitPrice(),
                "color", it.getColor(),
                "size", it.getSize(),
                "quantity", it.getQuantity()
        )).toList();

        return new OrderResponse(
                o.getId(), o.getUserId(), items,
                o.getSubTotal(), o.getShippingFee(), o.getTotal(),
                o.getPaymentMethod(), o.getStatus(),
                o.getPromptpayTarget(), o.getPromptpayQrUrl(),
                o.getExpiresAt(), o.getPaymentSlipUrl(),
                o.getCreatedAt(), o.getUpdatedAt()
        );
    }
}

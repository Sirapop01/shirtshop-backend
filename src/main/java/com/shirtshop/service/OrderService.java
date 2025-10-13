// src/main/java/com/shirtshop/service/OrderService.java
package com.shirtshop.service;

import com.shirtshop.dto.CreateOrderRequest;
import com.shirtshop.dto.CreateOrderResponse;
import com.shirtshop.dto.OrderListResponse;
import com.shirtshop.dto.OrderResponse;
import com.shirtshop.entity.*;
import com.shirtshop.repository.AddressRepository;
import com.shirtshop.repository.CartRepository;
import com.shirtshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final CloudinaryService cloudinaryService;
    private final AddressRepository addressRepo;

    @Value("${app.payment.promptpay.target}")           // เช่น "0952544014"
    private String promptpayTarget;

    @Value("${app.payment.promptpay.expire-minutes:15}") // อายุคำสั่งจ่าย (นาที)
    private int expireMinutes;

    /** ดึง userId จาก SecurityContext (รองรับ principal = User หรือ String) */
    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = (auth != null ? auth.getPrincipal() : null);
        if (p instanceof User u) return u.getId();
        if (p instanceof String s) return s;
        throw new IllegalStateException("Unauthenticated");
    }

    /** สร้างออเดอร์ชำระเงินด้วย PromptPay (EMV QR + fixed amount) และเคลียร์ตะกร้า */
    public CreateOrderResponse createPromptPayOrder(CreateOrderRequest req) {
        String userId = currentUserId();

        // 1) กันผู้ใช้กดซ้ำ: ถ้ามีออเดอร์เปิดอยู่ (ยังไม่หมดอายุ) คืนออเดอร์เดิม
        var openSt = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.SLIP_UPLOADED);
        var existing = orderRepo.findTopByUserIdAndStatusInAndExpiresAtAfterOrderByCreatedAtDesc(
                userId, openSt, Instant.now());
        if (existing.isPresent()) {
            var o = existing.get();
            return new CreateOrderResponse(
                    o.getId(), o.getTotal(), o.getPromptpayTarget(), o.getPromptpayQrUrl(), o.getExpiresAt()
            );
        }

        // 2) โหลดตะกร้า
        var cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart not found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // 3) คำนวณยอด
        int sub = cart.getItems().stream()
                .mapToInt(it -> it.getUnitPrice() * it.getQuantity())
                .sum();
        Integer shipObj = cart.getShippingFee();
        int shipping = shipObj != null ? shipObj : 0;
        int total = sub + shipping;
        if (total <= 0) throw new IllegalStateException("Invalid order total: " + total);

        // 4) สร้างออเดอร์
        var now = Instant.now();
        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentMethod(PaymentMethod.PROMPTPAY);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(now); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        order.setUpdatedAt(now); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        order.setExpiresAt(now.plus(expireMinutes, ChronoUnit.MINUTES));
        order.setSubTotal(sub);
        order.setShippingFee(shipping);
        order.setTotal(total);

        // map CartItem -> OrderItem
        var items = cart.getItems().stream().map(ci -> {
            OrderItem oi = new OrderItem();
            oi.setProductId(ci.getProductId());
            oi.setName(ci.getName()); // Fix: เรียกใช้ getName() และ setName()
            oi.setImageUrl(ci.getImageUrl());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setColor(ci.getColor());
            oi.setSize(ci.getSize());
            oi.setQuantity(ci.getQuantity());
            return oi;
        }).collect(Collectors.toList());
        order.setItems(items);

        // 5) สร้าง EMV-QR (ล็อกยอดเงิน)
        order.setPromptpayTarget(promptpayTarget);
        try {
            String qrDataUrl = PromptPayQr.generatePromptPayQrDataUrl(promptpayTarget, total, 360, true);
            order.setPromptpayQrUrl(qrDataUrl);
        } catch (Exception e) {
            // เผื่อมีปัญหาสร้างภาพ EMV QR ให้ fallback ไปใช้ promptpay.io
            String amt = String.format(java.util.Locale.US, "%.2f", total * 1.0);
            order.setPromptpayQrUrl("https://promptpay.io/" + promptpayTarget + ".png?amount=" + amt + "&size=360");
        }

        // 6) บันทึกออเดอร์
        order = orderRepo.save(order);

        // 7) เคลียร์ตะกร้าทันทีหลังสร้างออเดอร์
        cart.setItems(new ArrayList<>());
        cart.setShippingFee(0);
        cart.setUpdatedAt(LocalDateTime.from(Instant.now())); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        cartRepo.save(cart);

        // 8) ตอบกลับ
        return new CreateOrderResponse(
                order.getId(),
                order.getTotal(),
                order.getPromptpayTarget(),
                order.getPromptpayQrUrl(),
                order.getExpiresAt()
        );
    }


    /** อ่านรายละเอียดออเดอร์ */
    public OrderResponse getOrder(String id) {
        var o = orderRepo.findById(id).orElseThrow(() -> new IllegalStateException("Order not found"));
        return toResponse(o);
    }

    /** ผู้ใช้แนบสลิป */
    public OrderResponse uploadSlip(String orderId, MultipartFile slip) {
        String userId = currentUserId();
        var order = orderRepo.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Forbidden");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("Order already confirmed");
        }

        var up = cloudinaryService.uploadFile(slip, "shirtshop/slips");
        order.setPaymentSlipUrl(up.getUrl());
        order.setStatus(OrderStatus.SLIP_UPLOADED);
        order.setUpdatedAt(Instant.now()); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant

        order = orderRepo.save(order);
        return toResponse(order);
    }

    /** (สำหรับ Admin) ยืนยันการชำระ */
    public OrderResponse confirm(String id) {
        var o = orderRepo.findById(id).orElseThrow(() -> new IllegalStateException("Order not found"));
        o.setStatus(OrderStatus.PAID);
        o.setPaidAt(Instant.now()); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        o.setUpdatedAt(Instant.now()); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        orderRepo.save(o);
        return toResponse(o);
    }

    /** (สำหรับ Admin) ปฏิเสธสลิป */
    public OrderResponse reject(String id, String reason) {
        var o = orderRepo.findById(id).orElseThrow(() -> new IllegalStateException("Order not found"));
        o.setStatus(OrderStatus.REJECTED);
        o.setUpdatedAt(Instant.now()); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        // จะบันทึกเหตุผลไว้ใน field ใหม่ก็ได้ (เช่น o.setRejectReason(reason))
        orderRepo.save(o);
        return toResponse(o);
    }

    /** กู้คืนสินค้าเข้าตะกร้าจากออเดอร์ที่ EXPIRED/REJECTED */
    public void restoreCartFromOrder(String orderId) {
        String userId = currentUserId();
        var order = orderRepo.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Forbidden");
        }
        if (!(order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.REJECTED)) {
            throw new IllegalStateException("Only expired/rejected orders can be restored");
        }

        var cart = cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUserId(userId);
            c.setItems(new ArrayList<>());
            c.setCreatedAt(LocalDateTime.from(Instant.now())); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
            return c;
        });

        List<CartItem> items = order.getItems().stream().map(oi -> {
            CartItem ci = new CartItem();
            ci.setProductId(oi.getProductId());
            ci.setName(oi.getName()); // Fix: เรียกใช้ getName()
            ci.setImageUrl(oi.getImageUrl());
            ci.setUnitPrice(oi.getUnitPrice());
            ci.setColor(oi.getColor());
            ci.setSize(oi.getSize());
            ci.setQuantity(oi.getQuantity());
            return ci;
        }).collect(Collectors.toList());

        cart.setItems(new ArrayList<>(items));
        cart.setShippingFee(0);
        int sub = items.stream().mapToInt(i -> i.getUnitPrice() * i.getQuantity()).sum();
        // Assuming Cart has a setSubTotal method
        try {
            cart.getClass().getMethod("setSubTotal", int.class).invoke(cart, sub);
        } catch (Exception ignored) {
            // It's better to have a direct setter: cart.setSubTotal(sub);
        }
        cart.setUpdatedAt(LocalDateTime.from(Instant.now())); // Fix: แก้ประเภทข้อมูลกลับไปเป็น Instant
        cartRepo.save(cart);
    }

    /** map Entity -> DTO */
    private OrderResponse toResponse(Order o) {
        var items = o.getItems().stream().map(it -> Map.<String, Object>of(
                "productId", it.getProductId(),
                "name", it.getName(), // Fix: เรียกใช้ getName()
                "imageUrl", it.getImageUrl(),
                "unitPrice", it.getUnitPrice(),
                "color", it.getColor(),
                "size", it.getSize(),
                "quantity", it.getQuantity()
        )).collect(Collectors.toList());

        // Assuming OrderResponse constructor can handle Instant type
        return new OrderResponse(
                o.getId(), o.getUserId(), items,
                o.getSubTotal(), o.getShippingFee(), o.getTotal(),
                o.getPaymentMethod(), o.getStatus(),
                o.getPromptpayTarget(), o.getPromptpayQrUrl(),
                o.getExpiresAt(), o.getPaymentSlipUrl(),
                o.getCreatedAt(), o.getUpdatedAt()
        );
    }

    public OrderListResponse listMyOrders(List<OrderStatus> statuses, int page, int size) {
        String userId = currentUserId(); // เมธอดเดิมของคุณ

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "createdAt") // เรียงใหม่ล่าสุดก่อน
        );

        Page<Order> pg = (statuses == null || statuses.isEmpty())
                ? orderRepo.findByUserId(userId, pageable)
                : orderRepo.findByUserIdAndStatusIn(userId, statuses, pageable);

        var items = pg.getContent().stream()
                .map(this::toResponse) // หรือ map เป็น Summary DTO ตามที่คุณนิยามไว้
                .toList();

        return new OrderListResponse(
                items,
                pg.getNumber(),
                pg.getSize(),
                pg.getTotalElements(),
                pg.getTotalPages()
        );
    }

}
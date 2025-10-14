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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final CloudinaryService cloudinaryService;
    private final AddressRepository addressRepo;

    @Value("${app.payment.promptpay.target}")
    private String promptpayTarget;

    @Value("${app.payment.promptpay.expire-minutes:1}")
    private int expireMinutes;

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = (auth != null ? auth.getPrincipal() : null);
        if (p instanceof User u) return u.getId();
        if (p instanceof String s) return s;
        if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) return ud.getUsername();
        throw new IllegalStateException("Unauthenticated");
    }

    @Scheduled(fixedRate = 30000)
    public void cleanupExpiredOrders() {
        log.info("Running scheduled job: Cleaning up expired orders...");

        // 1. ค้นหาออเดอร์ทั้งหมดในระบบที่สถานะเป็น PENDING_PAYMENT และหมดอายุแล้ว
        List<Order> expiredOrders = orderRepo.findByStatusAndExpiresAtBefore(
                OrderStatus.PENDING_PAYMENT,
                Instant.now()
        );

        if (expiredOrders.isEmpty()) {
            log.info("No expired orders found.");
            return;
        }

        // 2. อัปเดตสถานะของออเดอร์ทั้งหมดที่พบ
        log.info("Found {} expired orders. Updating status to EXPIRED.", expiredOrders.size());
        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            order.setUpdatedAt(Instant.now());
        }

        // 3. บันทึกการเปลี่ยนแปลงลงฐานข้อมูล
        orderRepo.saveAll(expiredOrders);
        log.info("Successfully updated {} expired orders.", expiredOrders.size());
    }

    public CreateOrderResponse createPromptPayOrder(CreateOrderRequest req) {
        String userId = currentUserId();

        var openSt = List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.SLIP_UPLOADED);
        // Fix: ส่ง Instant.now() เข้าไปใน query ให้ตรงกับ Repository
        var existing = orderRepo.findTopByUserIdAndStatusInAndExpiresAtAfterOrderByCreatedAtDesc(
                userId, openSt, Instant.now());
        if (existing.isPresent()) {
            var o = existing.get();
            return new CreateOrderResponse(
                    o.getId(), o.getTotal(), o.getPromptpayTarget(), o.getPromptpayQrUrl(), o.getExpiresAt()
            );
        }

        var cart = cartRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Cart not found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        int sub = cart.getItems().stream()
                .mapToInt(it -> it.getUnitPrice() * it.getQuantity())
                .sum();
        Integer shipObj = cart.getShippingFee();
        int shipping = shipObj != null ? shipObj : 0;
        int total = sub + shipping;
        if (total <= 0) throw new IllegalStateException("Invalid order total: " + total);

        Instant now = Instant.now();
        Order order = new Order();
        order.setUserId(userId);
        order.setPaymentMethod(PaymentMethod.PROMPTPAY);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setExpiresAt(now.plus(expireMinutes, ChronoUnit.MINUTES));
        order.setSubTotal(sub);
        order.setShippingFee(shipping);
        order.setTotal(total);

        var items = cart.getItems().stream().map(ci -> {
            OrderItem oi = new OrderItem();
            oi.setProductId(ci.getProductId());
            String name = (ci.getProductName() != null && !ci.getProductName().isBlank())
                    ? ci.getProductName()
                    : ci.getName();
            oi.setName(name);
            oi.setImageUrl(ci.getImageUrl());
            oi.setUnitPrice(ci.getUnitPrice());
            oi.setColor(ci.getColor());
            oi.setSize(ci.getSize());
            oi.setQuantity(ci.getQuantity());
            return oi;
        }).collect(Collectors.toList());
        order.setItems(items);

        order.setPromptpayTarget(promptpayTarget);
        try {
            String qrDataUrl = PromptPayQr.generatePromptPayQrDataUrl(promptpayTarget, total, 360, true);
            order.setPromptpayQrUrl(qrDataUrl);
        } catch (Exception e) {
            String amt = String.format(java.util.Locale.US, "%.2f", total * 1.0);
            order.setPromptpayQrUrl("https://promptpay.io/" + promptpayTarget + ".png?amount=" + amt + "&size=360");
        }

        order = orderRepo.save(order);

        cart.setItems(new ArrayList<>());
        cart.setShippingFee(0);
        cart.setUpdatedAt(Instant.now());
        cartRepo.save(cart);

        return new CreateOrderResponse(
                order.getId(),
                order.getTotal(),
                order.getPromptpayTarget(),
                order.getPromptpayQrUrl(),
                order.getExpiresAt()
        );
    }

    public OrderResponse getOrder(String id) {
        var o = orderRepo.findById(id).orElseThrow(() -> new IllegalStateException("Order not found"));
        return toResponse(o);
    }

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
        order.setUpdatedAt(Instant.now());

        order = orderRepo.save(order);
        return toResponse(order);
    }

    private OrderResponse toResponse(Order o) {
        // แปลงรายการสินค้า (items) โดยมีการป้องกันค่า null
        var items = o.getItems().stream().map(it ->
                // ใช้วิธีตรวจสอบค่า null ก่อนส่งเข้า Map.of()
                // เพื่อป้องกัน NullPointerException
                Map.<String, Object>of(
                        "productId", it.getProductId() != null ? it.getProductId() : "",
                        "name", it.getName() != null ? it.getName() : "Unknown Product", // ใส่ค่าเริ่มต้นเผื่อชื่อเป็น null
                        "imageUrl", it.getImageUrl() != null ? it.getImageUrl() : "",
                        "unitPrice", it.getUnitPrice(), // สมมติว่า int/Integer ไม่เป็น null
                        "color", it.getColor() != null ? it.getColor() : "",
                        "size", it.getSize() != null ? it.getSize() : "",
                        "quantity", it.getQuantity() // สมมติว่า int/Integer ไม่เป็น null
                )
        ).collect(Collectors.toList());

        // สร้างและส่งคืน OrderResponse DTO
        // (ตรวจสอบให้แน่ใจว่า Constructor ของ OrderResponse ตรงกับพารามิเตอร์เหล่านี้)
        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                items,
                o.getSubTotal(),
                o.getShippingFee(),
                o.getTotal(),
                o.getPaymentMethod(),
                o.getStatus(),
                o.getPromptpayTarget(),
                o.getPromptpayQrUrl(),
                o.getExpiresAt(),
                o.getPaymentSlipUrl(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }

    public OrderListResponse listMyOrders(String userId, List<OrderStatus> statuses, Pageable pageable) {

        Page<Order> pg; // ประกาศตัวแปร Page
        if (statuses == null || statuses.isEmpty()) {
            // ✅ เรียก Repository โดยส่ง Pageable ไปตรงๆ
            pg = orderRepo.findByUserId(userId, pageable);
        } else {
            // ✅ เรียก Repository โดยส่ง Pageable ไปตรงๆ
            pg = orderRepo.findByUserIdAndStatusIn(userId, statuses, pageable);
        }

        // แปลง Page<Order> เป็น List<OrderResponse>
        var items = pg.getContent().stream()
                .map(this::toResponse)
                .toList();

        // ✅ สร้าง OrderListResponse จากข้อมูลใน Page object
        return new OrderListResponse(
                items,
                pg.getNumber(),      // ดึง page number จาก Page object
                pg.getSize(),        // ดึง page size จาก Page object
                pg.getTotalElements(),
                pg.getTotalPages()
        );
    }

    public void restoreCartFromOrder(String orderId) {
        // 1. ดึง ID ของผู้ใช้ปัจจุบันจาก Security Context
        String userId = currentUserId();

        // 2. ค้นหาออเดอร์จาก ID ที่ได้รับมา ถ้าไม่เจอให้โยน Exception
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found with id: " + orderId));

        // 3. ตรวจสอบความเป็นเจ้าของออเดอร์
        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Forbidden: User does not own this order");
        }

        // 4. ตรวจสอบสถานะของออเดอร์ว่าสามารถกู้คืนได้หรือไม่
        if (!(order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.REJECTED)) {
            throw new IllegalStateException("Only EXPIRED or REJECTED orders can be restored. Current status: " + order.getStatus());
        }

        // 5. ดึงตะกร้าสินค้าปัจจุบันของผู้ใช้ หรือสร้างใหม่ถ้ายังไม่มี
        Cart cart = cartRepo.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUserId(userId);
                    newCart.setItems(new ArrayList<>());
                    newCart.setCreatedAt(Instant.now());
                    return newCart;
                });

        // 6. แปลง OrderItems จากออเดอร์กลับไปเป็น CartItems
        // (เราจะสร้าง List ใหม่ เพื่อไม่ให้กระทบกับ List เดิมในตะกร้า)
        List<CartItem> itemsToRestore = order.getItems().stream()
                .map(orderItem -> {
                    CartItem cartItem = new CartItem();
                    cartItem.setProductId(orderItem.getProductId());
                    cartItem.setName(orderItem.getName());
                    cartItem.setImageUrl(orderItem.getImageUrl());
                    cartItem.setUnitPrice(orderItem.getUnitPrice());
                    cartItem.setColor(orderItem.getColor());
                    cartItem.setSize(orderItem.getSize());
                    cartItem.setQuantity(orderItem.getQuantity());
                    return cartItem;
                })
                .collect(Collectors.toList());

        // 7. ตั้งค่ารายการสินค้าใหม่ให้กับตะกร้า (เขียนทับของเดิม)
        cart.setItems(itemsToRestore);

        // 8. คำนวณยอดรวมใหม่ (ถ้ามีตรรกะนี้อยู่) และอัปเดตเวลา
        // recalc(cart); // หากคุณมีเมธอด recalc() ใน CartService ก็สามารถเรียกใช้ได้
        cart.setUpdatedAt(Instant.now());

        // 9. บันทึกตะกร้าที่อัปเดตแล้วลงฐานข้อมูล
        cartRepo.save(cart);
    }

    // === Admin: list orders ===
    public Page<Order> adminListOrders(OrderStatus status, Pageable pageable) {
        if (status == null) {
            // ทั้งหมด
            return orderRepo.findAll(pageable);
        }
        // กรองตามสถานะ
        return orderRepo.findAllByStatus(status, pageable);
    }

    // === Admin: approve / reject ===
    @org.springframework.transaction.annotation.Transactional
    public Order adminChangeStatus(String orderId, OrderStatus next,
                                   String adminUserId, String rejectReason) {
        var order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found"));

        var current = order.getStatus();

        // อนุญาตเฉพาะ: SLIP_UPLOADED -> PAID/REJECTED
        if (current != OrderStatus.SLIP_UPLOADED ||
                (next != OrderStatus.PAID && next != OrderStatus.REJECTED)) {
            throw new IllegalStateException("Invalid status transition: " + current + " -> " + next);
        }

        if (next == OrderStatus.PAID) {
            // ต้องมีสลิปก่อนถึงจะอนุมัติได้
            if (order.getPaymentSlipUrl() == null || order.getPaymentSlipUrl().isBlank()) {
                throw new IllegalStateException("Slip not uploaded");
            }
            // TODO: ตัดสต็อก/ออกใบเสร็จที่นี่ถ้าต้องการ
            // inventoryService.decrease(order.getItems());
        }

        if (next == OrderStatus.REJECTED) {
            // ถ้า entity มีฟิลด์ rejectReason ให้เซ็ต (ถ้ายังไม่มี ข้ามได้)
            try {
                var fld = order.getClass().getDeclaredField("rejectReason");
                fld.setAccessible(true);
                fld.set(order, rejectReason);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }

        // ถ้า entity มี verifiedBy/verifiedAt ให้เซ็ต (ไม่มีไม่เป็นไร)
        try {
            var vBy = order.getClass().getDeclaredField("verifiedBy");
            vBy.setAccessible(true);
            vBy.set(order, adminUserId);
        } catch (Exception ignored) {}

        try {
            var vAt = order.getClass().getDeclaredField("verifiedAt");
            vAt.setAccessible(true);
            vAt.set(order, Instant.now());
        } catch (Exception ignored) {}

        order.setStatus(next);
        order.setUpdatedAt(Instant.now());
        return orderRepo.save(order);
    }

}
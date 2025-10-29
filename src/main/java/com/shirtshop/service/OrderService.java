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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final CloudinaryService cloudinaryService;
    private final AddressRepository addressRepo;
    private final InventoryService inventoryService;

    // ⬇️ ใช้ค่าปัจจุบันจาก DB แทนการอ่านจาก application.yml
    private final PaymentSettingsService paymentSettingsService;

    /* ===================== Common ===================== */

    private String currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = (auth != null ? auth.getPrincipal() : null);
        if (p instanceof User u) return u.getId();
        if (p instanceof String s) return s;
        if (p instanceof org.springframework.security.core.userdetails.UserDetails ud) return ud.getUsername();
        throw new IllegalStateException("Unauthenticated");
    }

    /** สร้างเลขติดตามอ่านง่าย เช่น SHP-20251023-EE85CFA4 */
    private String generateTrackingTag(Order o) {
        String tail = (o.getId() != null && o.getId().length() >= 8)
                ? o.getId().substring(o.getId().length() - 8).toUpperCase()
                : Long.toHexString(System.nanoTime()).toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0, 8);

        String ymd = LocalDate.now(ZoneId.of("Asia/Bangkok"))
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd

        return "SHP-" + ymd + "-" + tail;
    }

    private static String safeNote(String note) {
        if (note == null) return null;
        String n = note.trim();
        return n.isEmpty() ? null : n;
    }

    /* =============== Scheduled cleanup =============== */
    @Scheduled(fixedRate = 30000)
    public void cleanupExpiredOrders() {
        log.info("Running scheduled job: Cleaning up expired orders...");

        List<Order> expiredOrders = orderRepo.findByStatusAndExpiresAtBefore(
                OrderStatus.PENDING_PAYMENT,
                Instant.now()
        );

        if (expiredOrders.isEmpty()) {
            log.info("No expired orders found.");
            return;
        }

        log.info("Found {} expired orders. Updating status to EXPIRED.", expiredOrders.size());
        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.EXPIRED);
            order.setUpdatedAt(Instant.now());
            // นโยบายคืนสต๊อกแล้วแต่ระบบของคุณ
        }

        orderRepo.saveAll(expiredOrders);
        log.info("Successfully updated {} expired orders.", expiredOrders.size());
    }

    /* =============== Create Order (PromptPay) =============== */

    // ⬇️ ใช้ค่าจาก PaymentSettings (DB) และ snapshot ลง Order
    public CreateOrderResponse createPromptPayOrder(CreateOrderRequest req) {
        String userId = currentUserId();

        // ===== 1) โหลด Address และทำ snapshot =====
        if (req.addressId() == null || req.addressId().isBlank()) {
            throw new IllegalArgumentException("addressId is required");
        }
        var address = addressRepo.findById(req.addressId())
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + req.addressId()));

        Order.ShippingAddress snap = new Order.ShippingAddress();
        snap.setRecipientName(tryStr(address, "getRecipientName", "getFullName", "getName"));
        snap.setPhone(tryStr(address, "getPhone", "getTel", "getMobile"));
        snap.setLine1(tryStr(address, "getLine1", "getAddressLine1", "getAddress1", "getLineOne"));
        snap.setLine2(tryStr(address, "getLine2", "getAddressLine2", "getAddress2", "getLineTwo"));
        snap.setSubDistrict(preferName(
                tryStr(address, "getSubDistrictName", "getSubdistrictName", "getTambonName", "getSubDistrict", "getSubdistrict", "getTambon"),
                tryStr(address, "getSubDistrictCode", "getSubdistrictCode")
        ));
        snap.setDistrict(preferName(
                tryStr(address, "getDistrictName", "getAmphurName", "getCityName", "getDistrictTh"),
                tryStr(address, "getDistrict", "getAmphur", "getCity")
        ));
        snap.setProvince(preferName(
                tryStr(address, "getProvinceName", "getProvinceTh", "getStateName"),
                tryStr(address, "getProvince", "getState")
        ));
        snap.setPostcode(tryStr(address, "getPostcode", "getZip", "getPostalCode"));

        String addrUserId = tryStr(address, "getUserId", "getUserid", "getOwnerId");

        // ===== 2) หา Cart แบบ "ห้ามสร้างใหม่" =====
        var cart = cartRepo.findByUserId(userId)
                .or(() -> Optional.ofNullable(addrUserId).flatMap(cartRepo::findByUserId))
                .orElseThrow(() -> new IllegalStateException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cart is empty");
        }

        // ===== 3) map cart -> order items =====
        List<OrderItem> orderItems = cart.getItems().stream().map(ci -> {
            OrderItem it = new OrderItem();
            it.setProductId(ci.getProductId());
            String itemName = tryStr(ci, "getName", "getProductName", "getTitle");
            it.setName(itemName);
            it.setImageUrl(ci.getImageUrl());
            it.setUnitPrice((int) ci.getUnitPrice());
            it.setColor(ci.getColor());
            it.setSize(ci.getSize());
            it.setQuantity(ci.getQuantity());
            return it;
        }).collect(Collectors.toList());

        int subTotal = orderItems.stream().mapToInt(i -> i.getUnitPrice() * i.getQuantity()).sum();
        int shippingFee = 0; // ปรับตามลอจิกเดิมของคุณ
        int total = subTotal + shippingFee;

        // ===== 4) โหลดค่าปัจจุบันจาก DB =====
        var ps = paymentSettingsService.getOrInit(); // target + expireMinutes
        String target = ps.getTarget();
        int expireMinutes = ps.getExpireMinutes();

        // ===== 5) build order & snapshot promptpay =====
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expireMinutes, ChronoUnit.MINUTES);

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(orderItems);
        order.setSubTotal(subTotal);
        order.setShippingFee(shippingFee);
        order.setTotal(total);
        order.setPaymentMethod(PaymentMethod.PROMPTPAY);
        order.setStatus(OrderStatus.PENDING_PAYMENT);

        order.setPromptpayTarget(target);
        try {
            String qrDataUrl = PromptPayQr.generatePromptPayQrDataUrl(target, total, 360, true);
            order.setPromptpayQrUrl(qrDataUrl);
        } catch (Exception e) {
            String amt = String.format(java.util.Locale.US, "%.2f", total * 1.0);
            order.setPromptpayQrUrl("https://promptpay.io/" + target + ".png?amount=" + amt + "&size=360");
        }

        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        order.setExpiresAt(expiresAt);

        // ===== 6) แนบ Address ใส่ Order =====
        order.setAddressId(req.addressId());
        order.setShippingAddress(snap);

        order.setStockAdjusted(false);
        order.setStockRestored(false);

        order = orderRepo.save(order);

        // ===== 7) เคลียร์ตะกร้า =====
        cart.getItems().clear();
        cartRepo.save(cart);

        return new CreateOrderResponse(
                order.getId(),
                order.getTotal(),
                order.getPromptpayTarget(),
                order.getPromptpayQrUrl(),
                order.getExpiresAt()
        );
    }

    /* =============== User actions =============== */

    public OrderResponse getOrder(String id) {
        var o = orderRepo.findById(id).orElseThrow(() -> new IllegalStateException("Order not found"));
        return mapToOrderResponse(o);
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

        // ตัดสต๊อกตอน slip uploaded (ครั้งเดียว) ตามตรรกะเดิม
        inventoryService.deductOnSlipUploaded(order);

        order = orderRepo.save(order);
        return mapToOrderResponse(order);
    }

    /* =============== List my orders =============== */

    public OrderListResponse listMyOrders(String userId, List<OrderStatus> statuses, Pageable pageable) {
        Page<Order> pg;
        if (statuses == null || statuses.isEmpty()) {
            pg = orderRepo.findByUserId(userId, pageable);
        } else {
            pg = orderRepo.findByUserIdAndStatusIn(userId, statuses, pageable);
        }

        var items = pg.getContent().stream()
                .map(this::mapToOrderResponse)
                .toList();

        return new OrderListResponse(
                items,
                pg.getNumber(),
                pg.getSize(),
                pg.getTotalElements(),
                pg.getTotalPages()
        );
    }

    // เรียกจาก AdminOrderController.list(...)
    public OrderListResponse adminList(String keyword,
                                       java.util.List<OrderStatus> statuses,
                                       org.springframework.data.domain.Pageable pageable) {
        var page = orderRepo.findAll(pageable);

        var filtered = page.getContent().stream()
                .filter(o -> (statuses == null || statuses.isEmpty()) || statuses.contains(o.getStatus()))
                .filter(o -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    var k = keyword.toLowerCase();
                    return (o.getId() != null && o.getId().toLowerCase().contains(k))
                            || (o.getUserId() != null && o.getUserId().toLowerCase().contains(k))
                            || (o.getTrackingTag() != null && o.getTrackingTag().toLowerCase().contains(k))
                            || (o.getStatusNote() != null && o.getStatusNote().toLowerCase().contains(k));
                })
                .toList();

        var data = filtered.stream().map(this::mapToOrderResponse).toList();
        var newPage = new org.springframework.data.domain.PageImpl<>(data, pageable, data.size());

        return new OrderListResponse(
                newPage.getContent(),
                newPage.getNumber(),
                newPage.getSize(),
                newPage.getTotalElements(),
                newPage.getTotalPages()
        );
    }

    /* =============== Restore cart from order =============== */

    public void restoreCartFromOrder(String orderId) {
        String userId = currentUserId();
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found with id: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("Forbidden: User does not own this order");
        }

        if (!(order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.REJECTED)) {
            throw new IllegalStateException("Only EXPIRED or REJECTED orders can be restored. Current status: " + order.getStatus());
        }

        Cart cart = cartRepo.findByUserId(userId)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setItems(new ArrayList<>());
                    c.setCreatedAt(Instant.now());
                    return c;
                });

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

        cart.setItems(itemsToRestore);
        cart.setUpdatedAt(Instant.now());
        cartRepo.save(cart);
    }

    /* =============== Admin list =============== */

    public Page<Order> adminListOrders(OrderStatus status, Pageable pageable) {
        if (status == null) {
            return orderRepo.findAll(pageable);
        }
        return orderRepo.findAllByStatus(status, pageable);
    }

    /* =============== Admin change status (Approve/Reject/Cancel) =============== */

    public OrderResponse adminChangeStatus(String orderId,
                                           OrderStatus newStatus,
                                           String adminId,
                                           String note) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (newStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        switch (newStatus) {
            case PAID -> {
                if (order.getStatus() != OrderStatus.SLIP_UPLOADED) {
                    throw new IllegalArgumentException("Only SLIP_UPLOADED can be approved (to PAID)");
                }
                order.setStatus(OrderStatus.PAID);
                order.setStatusNote(null);

                if (order.getTrackingTag() == null || order.getTrackingTag().isBlank()) {
                    order.setTrackingTag(generateTrackingTag(order));
                    order.setTrackingCreatedAt(Instant.now());
                }

                order.setUpdatedAt(Instant.now());
            }

            case REJECTED -> {
                if (order.getStatus() == OrderStatus.REJECTED || order.getStatus() == OrderStatus.CANCELED) {
                    throw new IllegalArgumentException("Order already closed");
                }
                inventoryService.restoreOnClosed(order);
                order.setStatus(OrderStatus.REJECTED);
                order.setStatusNote(safeNote(note));
                order.setUpdatedAt(Instant.now());
            }

            case CANCELED -> {
                if (order.getStatus() == OrderStatus.REJECTED || order.getStatus() == OrderStatus.CANCELED) {
                    throw new IllegalArgumentException("Order already closed");
                }
                inventoryService.restoreOnClosed(order);
                order.setStatus(OrderStatus.CANCELED);
                order.setStatusNote(safeNote(note));
                order.setUpdatedAt(Instant.now());
            }

            default -> throw new IllegalArgumentException("Unsupported status: " + newStatus);
        }

        Order saved = orderRepo.save(order);
        return mapToOrderResponse(saved);
    }

    /* =============== Mapping =============== */

    private OrderResponse mapToOrderResponse(Order o) {
        List<Map<String, Object>> itemsPayload = o.getItems() == null ? List.of() :
                o.getItems().stream().map(it -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", it.getProductId());
                    m.put("name", it.getName());
                    m.put("imageUrl", it.getImageUrl());
                    m.put("unitPrice", it.getUnitPrice());
                    m.put("color", it.getColor());
                    m.put("size", it.getSize());
                    m.put("quantity", it.getQuantity());
                    return m;
                }).toList();

        Map<String, Object> addrPayload = null;
        Order.ShippingAddress sa = o.getShippingAddress();
        if (sa != null) {
            addrPayload = new LinkedHashMap<>();
            addrPayload.put("recipientName", sa.getRecipientName());
            addrPayload.put("phone", sa.getPhone());
            addrPayload.put("line1", sa.getLine1());
            addrPayload.put("line2", sa.getLine2());
            addrPayload.put("subDistrict", sa.getSubDistrict());
            addrPayload.put("district", sa.getDistrict());
            addrPayload.put("province", sa.getProvince());
            addrPayload.put("postcode", sa.getPostcode());
        }

        return new OrderResponse(
                o.getId(),
                o.getUserId(),
                itemsPayload,
                o.getSubTotal(),
                o.getShippingFee(),
                o.getTotal(),
                o.getPaymentMethod(),
                o.getStatus(),
                o.getPromptpayTarget(),
                o.getPromptpayQrUrl(),
                o.getExpiresAt(),
                o.getPaymentSlipUrl(),
                o.getAddressId(),
                addrPayload,
                o.getTrackingTag(),
                o.getTrackingCreatedAt(),
                o.getStatusNote(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }

    /* =============== Utils =============== */

    private static int tryInt(Object target, String... methodNames) {
        if (target == null) return 0;
        for (String name : methodNames) {
            try {
                Method m = target.getClass().getMethod(name);
                Object v = m.invoke(target);
                if (v instanceof Number n) return n.intValue();
            } catch (Exception ignored) { /* next */ }
        }
        return 0;
    }

    private static String tryStr(Object target, String... methodNames) {
        if (target == null) return null;
        for (String name : methodNames) {
            try {
                var m = target.getClass().getMethod(name);
                var v = m.invoke(target);
                if (v != null) return String.valueOf(v);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean isDigits(String s) {
        if (s == null) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return s.length() > 0;
    }

    /** เลือก name ก่อนถ้าเป็น "คำ" (ไม่ใช่ตัวเลขล้วน) ไม่งั้น fallback เป็น code */
    private static String preferName(String name, String code) {
        if (name != null && !name.isBlank() && !isDigits(name)) return name;
        if (code != null && !code.isBlank() && !isDigits(code)) return code;
        return (name != null && !name.isBlank()) ? name : code;
    }
}

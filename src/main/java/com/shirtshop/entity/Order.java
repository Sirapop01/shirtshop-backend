// src/main/java/com/shirtshop/entity/Order.java
package com.shirtshop.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    private String userId;

    // สินค้าที่สั่งซื้อ
    private List<OrderItem> items = new ArrayList<>();

    // ยอดรวม
    private int subTotal;
    private int shippingFee;
    private int total;

    // การชำระเงิน/สถานะ
    private PaymentMethod paymentMethod;
    private OrderStatus status;

    // PromptPay
    private String promptpayTarget;
    private String promptpayQrUrl;

    // Slip โอนเงินของลูกค้า
    private String paymentSlipUrl;

    // 🔹 Address snapshot (เพิ่มใหม่)
    private String addressId;
    private ShippingAddress shippingAddress;

    // Tracking
    private String trackingTag;        // เช่น SHP-20251022-EE85CFA4
    private Instant trackingCreatedAt;

    // Audit / เวลา
    private String statusNote;
    private String verifiedBy;         // userId ของแอดมินที่กด
    private Instant verifiedAt;        // เวลาอนุมัติ/ปฏิเสธ
    private boolean stockAdjusted;     // เคยตัดสต๊อกแล้ว
    private boolean stockRestored;     // เคยคืนสต๊อกแล้ว
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant expiresAt;

    // ====== Address snapshot object ======
    @Data
    public static class ShippingAddress {
        private String recipientName;
        private String phone;
        private String line1;
        private String line2;
        private String subDistrict;
        private String district;
        private String province;
        private String postcode;
    }
}

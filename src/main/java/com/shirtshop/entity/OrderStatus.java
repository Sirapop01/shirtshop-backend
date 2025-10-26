// src/main/java/com/shirtshop/entity/OrderStatus.java
package com.shirtshop.entity;
public enum OrderStatus {
    PENDING_PAYMENT,      // สร้างออเดอร์แล้ว รอชำระ
    SLIP_UPLOADED,        // ลูกค้าอัปสลิปแล้ว
    PAID,                 // แอดมินยืนยันเรียบร้อย
    REJECTED,             // แอดมินปฏิเสธสลิป
    EXPIRED,               // หมดอายุการชำระ
    CANCELED
}

// (สำหรับ merge ตอนล็อกอิน)
// src/main/java/com/shirtshop/dto/cart/MergeCartRequest.java
package com.shirtshop.dto.cart;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MergeCartRequest {
    private List<AddCartItemRequest> items; // เอาของจาก localStorage มารวม
}

// src/main/java/com/shirtshop/dto/TopProductResponse.java
package com.shirtshop.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TopProductResponse {
    private String id;
    private String name;
    private long units;           // จำนวนชิ้นที่ขาย (ช่วงเวลาที่ขอ)
    private BigDecimal revenue;   // รายได้ = sum(units * price)
}

// src/main/java/com/shirtshop/dto/DashboardSummaryResponse.java
package com.shirtshop.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DashboardSummaryResponse {
    private BigDecimal revenue;     // รวมรายได้ (ค่าเริ่มต้นใช้ subTotal; เปลี่ยนเป็น total ได้ ดู Service)
    private long orders;            // จำนวนออเดอร์ที่เป็น PAID
    private BigDecimal averageOrderValue; // revenue / orders (ถ้า orders=0 -> 0)
}

// src/main/java/com/shirtshop/dto/RevenuePointResponse.java
package com.shirtshop.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenuePointResponse {
    private LocalDate date;       // วัน (ตามโซนเวลา)
    private BigDecimal revenue;   // รายได้รวมของวันนั้น
}

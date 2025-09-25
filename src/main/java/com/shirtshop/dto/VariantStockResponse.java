// ใหม่: VariantStockResponse.java
package com.shirtshop.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantStockResponse {
    private String color;
    private String size;
    private int quantity;
}


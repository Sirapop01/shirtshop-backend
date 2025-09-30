package com.shirtshop.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LowStockItemResponse {
    private String id;
    private String name;
    private Integer stock;   // จำนวนคงเหลือรวม
    private String sku;      // ถ้าไม่มี ใช้ null ไปก่อน
}

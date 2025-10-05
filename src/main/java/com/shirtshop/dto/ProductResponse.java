package com.shirtshop.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;

    private int price;

    private String category;
    private List<String> imageUrls;

    // ใช้ ImageInfo แยกเป็น class ของมันเอง (ไม่ซ้อนใน ProductResponse)
    private List<ImageInfo> images;

    private List<String> availableColors;
    private List<String> availableSizes;
    private Integer stockQuantity;

    // ใช้ LocalDateTime ให้ตรง entity
    private java.time.LocalDateTime createdAt;

    private List<VariantStockResponse> variantStocks;
}

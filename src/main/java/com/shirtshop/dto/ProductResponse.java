// ProductResponse.java
package com.shirtshop.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;

    // เดิม
    private List<String> imageUrls;
    private List<String> availableColors;
    private List<String> availableSizes;
    private int stockQuantity;
    private LocalDateTime createdAt;

    // ใหม่
    private List<ImageInfo> images;           // [{publicId,url}]
    private List<VariantStockResponse> variantStocks;
}



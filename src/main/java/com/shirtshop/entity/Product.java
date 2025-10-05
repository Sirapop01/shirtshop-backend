package com.shirtshop.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "products")
public class Product {
    @Id private String id;
    private String name;
    private String description;
    private int price;
    private String category;
    private List<String> imageUrls;
    private List<String> imagePublicIds;
    private List<String> availableColors;
    private List<String> availableSizes;
    private int stockQuantity;
    private List<VariantStock> variantStocks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
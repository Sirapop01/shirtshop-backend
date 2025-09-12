package com.shirtshop.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private List<String> availableColors;
    private List<String> availableSizes;
    private int stockQuantity;
}
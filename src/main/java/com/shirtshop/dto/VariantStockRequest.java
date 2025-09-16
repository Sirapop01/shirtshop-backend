package com.shirtshop.dto;

import lombok.Data;

@Data
public class VariantStockRequest {
    private String color;
    private String size;
    private int quantity;
}
// ใหม่: VariantStockResponse.java
package com.shirtshop.dto;
import lombok.Data;

@Data
public class VariantStockResponse {
    private String color;
    private String size;
    private int quantity;
}
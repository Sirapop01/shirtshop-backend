// src/main/java/com/shirtshop/dto/CategoryBreakdownResponse.java
package com.shirtshop.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryBreakdownResponse {
    private String category; // ชื่อหมวด
    private long value;      // จำนวนชิ้นรวมของหมวด
}

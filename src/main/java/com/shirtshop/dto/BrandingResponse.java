// src/main/java/com/shirtshop/dto/BrandingResponse.java
package com.shirtshop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class BrandingResponse {
    private String siteName;
    private String logoUrl; // อาจเป็น null
}
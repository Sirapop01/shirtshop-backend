// src/main/java/com/shirtshop/dto/BrandingUpdateRequest.java
package com.shirtshop.dto;

import lombok.Data;

@Data
public class BrandingUpdateRequest {
    private String siteName;
    private Boolean removeLogo; // true = ลบโลโก้เดิม
}
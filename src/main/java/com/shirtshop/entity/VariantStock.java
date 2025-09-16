// ใหม่: com.shirtshop.entity.VariantStock.java
package com.shirtshop.entity;

import lombok.Data;

@Data
public class VariantStock {
    private String color; // ต้องเป็นหนึ่งใน availableColors
    private String size;  // ต้องเป็นหนึ่งใน availableSizes
    private int quantity; // จำนวนของสี+ไซส์นี้
}

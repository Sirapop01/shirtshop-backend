// src/main/java/com/shirtshop/dto/CartResponse.java
package com.shirtshop.dto.cart;

import java.util.List;
import java.util.Map;

/**
 * โครงสร้างที่ FE คาดหวัง:
 * {
 *   items: [{ productId, name, imageUrl, unitPrice, color, size, quantity }],
 *   subTotal: number,
 *   shippingFee: number
 * }
 */
public record CartResponse(
        List<Map<String, Object>> items,
        int subTotal,
        int shippingFee
) {}

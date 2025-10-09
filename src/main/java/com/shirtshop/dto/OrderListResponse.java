// dto เล็ก ๆ สำหรับเพจรายชื่อ
package com.shirtshop.dto;
import java.util.List;

public record OrderListResponse(
        List<OrderResponse> items,
        int page, int size, long totalElements, int totalPages
) {}

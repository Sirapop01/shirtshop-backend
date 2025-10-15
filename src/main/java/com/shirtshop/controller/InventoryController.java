// src/main/java/com/shirtshop/controller/InventoryController.java
package com.shirtshop.controller;

import com.shirtshop.dto.LowStockItemResponse;
import com.shirtshop.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Low Stock for Dashboard
     * รองรับได้ทั้ง:
     *  - /api/inventory/low-stock?limit=6
     *  - /api/inventory/low-stock?page=0&size=6
     *  - เพิ่ม threshold ได้ เช่น /api/inventory/low-stock?limit=6&threshold=5
     */
    @GetMapping("/low-stock")
    public List<LowStockItemResponse> getLowStock(
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) Integer threshold
    ) {
        // ถ้าส่ง limit มาก็ใช้ limit เป็น size และบังคับหน้า 0
        if (limit != null && limit > 0) {
            page = 0;
            size = limit;
        }
        return inventoryService.getLowStock(page, size, threshold);
    }
}

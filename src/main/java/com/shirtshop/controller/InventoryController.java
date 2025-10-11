package com.shirtshop.controller;

import com.shirtshop.dto.LowStockItemResponse;
import com.shirtshop.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    /** GET /api/inventory/low-stock?threshold=5&limit=6 */
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockItemResponse>> lowStock(
            @RequestParam(defaultValue = "5") int threshold,
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(inventoryService.getLowStock(threshold, limit));
    }
}

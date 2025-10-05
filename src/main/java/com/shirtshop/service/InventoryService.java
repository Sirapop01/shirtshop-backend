package com.shirtshop.service;

import com.shirtshop.dto.LowStockItemResponse;
import com.shirtshop.entity.Product;
import com.shirtshop.entity.VariantStock;
import com.shirtshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;

    /**
     * ดึงรายการสินค้าที่ Low stock
     * @param threshold เกณฑ์ต่ำสุด (เช่น 5 = แสดงสินค้าที่สต็อก <= 5)
     * @param limit จำนวนรายการสูงสุด
     */
    public List<LowStockItemResponse> getLowStock(int threshold, int limit) {
        return productRepository.findAll().stream()
                .map(p -> LowStockItemResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .stock(calcTotalStock(p))
                        .sku(null) // ถ้ามีโครง SKU ของคุณเอง ค่อย map ตรงนี้
                        .build())
                .filter(item -> item.getStock() != null && item.getStock() <= threshold)
                .sorted(Comparator.comparingInt(LowStockItemResponse::getStock)) // น้อยก่อน
                .limit(Math.max(1, limit))
                .toList();
    }

    /** รวมสต็อกจาก product:
     *  - ถ้ามี variantStocks → รวม quantity ทั้งหมด (null นับเป็น 0)
     *  - ถ้าไม่มี → ใช้ stockQuantity (primitive int) ได้เลย
     */
    private int calcTotalStock(Product p) {
        List<VariantStock> vs = p.getVariantStocks();
        if (vs != null && !vs.isEmpty()) {
            return vs.stream()
                    .map(VariantStock::getQuantity)     // อาจเป็น Integer
                    .map(q -> q == null ? 0 : q)        // กัน null
                    .reduce(0, Integer::sum);
        }
        // stockQuantity เป็น int (primitive) ไม่มีวันเป็น null
        return Math.max(0, p.getStockQuantity());
    }
}

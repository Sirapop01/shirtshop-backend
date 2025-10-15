// src/main/java/com/shirtshop/service/InventoryService.java
package com.shirtshop.service;

import com.shirtshop.dto.LowStockItemResponse;
import com.shirtshop.entity.Order;
import com.shirtshop.entity.OrderItem;
import com.shirtshop.entity.Product;
import com.shirtshop.entity.VariantStock;
import com.shirtshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;

    // ========================= Bridge (รองรับเมธอดเดิม) =========================
    /** เดิม: ตัดสต๊อกด้วยรายการ items */
    @Transactional
    public void deductStockForOrderItems(List<OrderItem> items) {
        adjust(items, true);
    }

    /** เดิม: คืนสต๊อกด้วยรายการ items */
    @Transactional
    public void restoreStockForOrderItems(List<OrderItem> items) {
        adjust(items, false);
    }

    // ========================= Flow ใหม่ตามที่ตกลง =========================
    /** ตัดสต๊อกตอนผู้ใช้อัปสลิป (SLIP_UPLOADED) */
    @Transactional
    public void deductOnSlipUploaded(Order order) {
        if (order == null || order.getItems() == null) return;
        adjust(order.getItems(), true);
        // ถ้ามี flag ใน Order: order.setStockAdjusted(true); order.setStockRestored(false);
    }

    /** คืนสต๊อกเมื่อ Admin REJECT หรือ CANCELED */
    @Transactional
    public void restoreOnClosed(Order order) {
        if (order == null || order.getItems() == null) return;
        adjust(order.getItems(), false);
        // ถ้ามี flag ใน Order: order.setStockRestored(true);
    }

    // ========================= Core ปรับสต๊อก (คีย์ = productId + color + size) =========================
    private void adjust(List<OrderItem> items, boolean isDeduct) {
        if (items == null || items.isEmpty()) return;

        // รวมจำนวนต่อ variant (productId + color + size)
        Map<Key, Integer> qtyByKey = new HashMap<>();
        for (OrderItem it : items) {
            if (it == null) continue;

            String productId = it.getProductId();
            String color = safe(it.getColor());
            String size  = safe(it.getSize());
            int qty = it.getQuantity();

            if (productId == null || qty <= 0) continue;

            Key k = new Key(productId, color, size);
            qtyByKey.merge(k, qty, Integer::sum);
        }
        if (qtyByKey.isEmpty()) return;

        // โหลดสินค้าที่เกี่ยวข้องทั้งหมดครั้งเดียว
        Set<String> productIds = qtyByKey.keySet().stream().map(k -> k.productId).collect(Collectors.toSet());
        List<Product> products = productRepository.findAllById(productIds);
        Map<String, Product> productById = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // ถ้าจะ "ตัด" สต๊อก ให้ตรวจพอก่อน
        if (isDeduct) {
            for (Map.Entry<Key, Integer> e : qtyByKey.entrySet()) {
                Product p = productById.get(e.getKey().productId);
                if (p == null) {
                    throw new IllegalStateException("Product not found: " + e.getKey().productId);
                }
                VariantStock vs = findVariantByColorSize(p, e.getKey().color, e.getKey().size);
                int need = e.getValue();
                if (vs.getQuantity() < need) {
                    throw new IllegalStateException(
                            "Insufficient stock: product=" + p.getId()
                                    + ", color=" + e.getKey().color
                                    + ", size="  + e.getKey().size
                                    + " (need " + need + ", have " + vs.getQuantity() + ")"
                    );
                }
            }
        }

        // ปรับจริง (กันติดลบ)
        for (Map.Entry<Key, Integer> e : qtyByKey.entrySet()) {
            Product p = productById.get(e.getKey().productId);
            if (p == null) continue; // หรือโยน exception ตาม policy

            VariantStock vs = findVariantByColorSize(p, e.getKey().color, e.getKey().size);
            int current = vs.getQuantity();
            int delta   = e.getValue();
            int nextQty = isDeduct ? (current - delta) : (current + delta);
            if (nextQty < 0) {
                throw new IllegalStateException("Quantity would be negative for product=" + p.getId()
                        + ", color=" + e.getKey().color + ", size=" + e.getKey().size);
            }
            vs.setQuantity(nextQty);
        }

        // ✅ อัปเดต stockQuantity รวมของสินค้าให้เท่าผลรวม variant ทุกตัว
        for (Product p : products) {
            recomputeTotalStock(p);
        }

        productRepository.saveAll(products);
    }

    /** หา VariantStock ด้วย color + size (null-safe) */
    private VariantStock findVariantByColorSize(Product p, String color, String size) {
        List<VariantStock> list = p.getVariantStocks();
        if (list == null) {
            throw new IllegalStateException("No variant list for product=" + p.getId());
        }
        String c = safe(color);
        String s = safe(size);

        return list.stream()
                .filter(v -> Objects.equals(safe(v.getColor()), c) && Objects.equals(safe(v.getSize()), s))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Variant not found: product=" + p.getId() + ", color=" + color + ", size=" + size));
    }

    /** คำนวณสต๊อกรวมใหม่จากทุกรายการ variant แล้วอัปเดตลง Product.stockQuantity */
    private void recomputeTotalStock(Product p) {
        int total = 0;
        List<VariantStock> list = p.getVariantStocks();
        if (list != null) {
            for (VariantStock v : list) {
                if (v != null) total += Math.max(0, v.getQuantity());
            }
        }
        p.setStockQuantity(total);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /** คีย์ระบุ variant โดยใช้ productId + color + size */
    private record Key(String productId, String color, String size) {}

    // ========================= Low Stock Report =========================
    /**
     * รายการสต๊อกต่ำ (<= threshold) — ใช้ DTO: LowStockItemResponse(id, name, stock, sku)
     * ตอนนี้ Variant ไม่มี sku → ใส่ค่า null ไปก่อน
     */
    @Transactional(readOnly = true)
    public List<LowStockItemResponse> getLowStock(int page, int size, Integer thresholdOpt) {
        final int threshold = (thresholdOpt != null ? thresholdOpt : 5);

        List<Product> all = productRepository.findAll();

        List<LowStockItemResponse> result = new ArrayList<>();
        for (Product p : all) {
            int totalQty = 0;
            List<VariantStock> variants = p.getVariantStocks();
            if (variants != null) {
                for (VariantStock v : variants) {
                    if (v != null) totalQty += Math.max(0, v.getQuantity());
                }
            }
            if (totalQty <= threshold) {
                result.add(LowStockItemResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .stock(totalQty)
                        .sku(null) // ไม่มี sku ใน VariantStock ตอนนี้
                        .build());
            }
        }

        // เรียงจากสต็อกน้อย → มาก
        result.sort(Comparator.comparing(LowStockItemResponse::getStock));

        // Pagination
        int from = Math.max(0, page * size);
        int to   = Math.min(result.size(), from + size);
        if (from >= to) return List.of();
        return result.subList(from, to);
    }
}

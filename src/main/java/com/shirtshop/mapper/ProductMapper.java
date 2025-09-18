package com.shirtshop.mapper;

import com.shirtshop.dto.ProductResponse;
import com.shirtshop.dto.VariantStockResponse;
import com.shirtshop.entity.Product;
import com.shirtshop.entity.VariantStock;

import java.util.stream.Collectors;

public class ProductMapper {

    public static ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .category(p.getCategory())
                .price(p.getPrice())
                .imageUrls(p.getImageUrls())
                .variantStocks(
                        p.getVariantStocks()
                                .stream()
                                .map(ProductMapper::toVariant)
                                .collect(Collectors.toList())
                )
                .build();
    }

    public static VariantStockResponse toVariant(VariantStock v) {
        return VariantStockResponse.builder()
                .color(v.getColor() != null ? v.getColor().trim() : "")
                .size(v.getSize() != null ? v.getSize().trim() : "")
                .quantity(v.getQuantity() != null ? v.getQuantity() : 0)
                .build();
    }
}

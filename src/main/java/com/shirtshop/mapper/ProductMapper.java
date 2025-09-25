package com.shirtshop.mapper;

import com.shirtshop.dto.*;
import com.shirtshop.entity.Product;
import com.shirtshop.entity.VariantStock;
import com.shirtshop.dto.ImageInfo;

import java.util.stream.Collectors;

public class ProductMapper {

    public static ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice()) // BigDecimal → BigDecimal ตรงกัน
                .category(p.getCategory())
                .imageUrls(p.getImageUrls())
                .availableColors(p.getAvailableColors())
                .availableSizes(p.getAvailableSizes())
                .stockQuantity(p.getStockQuantity())
                .createdAt(p.getCreatedAt()) // LocalDateTime → LocalDateTime
                .variantStocks(p.getVariantStocks() == null ? null :
                        p.getVariantStocks().stream()
                                .map(ProductMapper::toVariant)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private static VariantStockResponse toVariant(VariantStock v) {
        return VariantStockResponse.builder()
                .color(v.getColor())
                .size(v.getSize())
                .quantity(v.getQuantity())
                .build();
    }

    private static ImageInfo toImageInfo(ImageInfo img) {
        return ImageInfo.builder()
                .publicId(img.getPublicId())
                .url(img.getUrl())
                .build();
    }
}

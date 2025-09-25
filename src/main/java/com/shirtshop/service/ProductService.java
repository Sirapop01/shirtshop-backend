package com.shirtshop.service;


import com.shirtshop.dto.*;
import com.shirtshop.entity.Product;
import com.shirtshop.entity.VariantStock;
import com.shirtshop.exception.ResourceNotFoundException;
import com.shirtshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService; // Inject Service สำหรับอัปโหลด

    /**
     * ดึงข้อมูลสินค้าทั้งหมด
     * @return รายการสินค้าทั้งหมดในรูปแบบ ProductResponse
     */
    public List<ProductResponse> getAllProducts() {
        // ดึงข้อมูลสินค้าทั้งหมดจากฐานข้อมูล
        List<Product> products = productRepository.findAll();

        // ใช้ Stream API เพื่อแปลง Product Entity ทุกตัวให้เป็น ProductResponse DTO
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public Product getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }


    public void deleteProduct(String productId) {
        // 1. ค้นหาสินค้า
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // 2. ลบรูปภาพทั้งหมดใน Cloudinary
        if (product.getImagePublicIds() != null && !product.getImagePublicIds().isEmpty()) {
            for (String publicId : product.getImagePublicIds()) {
                cloudinaryService.deleteFile(publicId);
            }
        }

        // 3. ลบข้อมูลสินค้าออกจาก MongoDB
        productRepository.delete(product);
    }

    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> images) {
        List<String> imageUrls = new ArrayList<>();
        List<String> imagePublicIds = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                CloudinaryUploadResponse resp = cloudinaryService.uploadFile(image, "products");
                imageUrls.add(resp.getUrl());
                imagePublicIds.add(resp.getPublicId());
            }
        }

        Product p = new Product();
        p.setName(productRequest.getName());
        p.setDescription(productRequest.getDescription());
        p.setPrice(productRequest.getPrice());
        p.setCategory(productRequest.getCategory());
        p.setAvailableColors(productRequest.getAvailableColors());
        p.setAvailableSizes(productRequest.getAvailableSizes());
        p.setImageUrls(imageUrls);
        p.setImagePublicIds(imagePublicIds);

        // variantStocks
        if (productRequest.getVariantStocks() != null) {
            List<VariantStock> variants = productRequest.getVariantStocks().stream().map(v -> {
                VariantStock vs = new VariantStock();
                vs.setColor(v.getColor());
                vs.setSize(v.getSize());
                vs.setQuantity(v.getQuantity());
                return vs;
            }).collect(Collectors.toList());
            p.setVariantStocks(variants);

            // คำนวณ stock รวม
            int sum = variants.stream().mapToInt(VariantStock::getQuantity).sum();
            p.setStockQuantity(sum);
        } else {
            // fallback: กรณีเก่ายังส่ง stockQuantity มาอย่างเดียว
            p.setStockQuantity(productRequest.getStockQuantity());
        }

        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        Product saved = productRepository.save(p);
        return mapToProductResponse(saved);
    }

    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse r = new ProductResponse();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setDescription(product.getDescription());
        r.setPrice(product.getPrice());
        r.setCategory(product.getCategory());
        r.setStockQuantity(product.getStockQuantity());
        r.setCreatedAt(product.getCreatedAt());

        // จัดการเรื่องรูปภาพ
        r.setImageUrls(product.getImageUrls() != null ? product.getImageUrls() : Collections.emptyList());
        if (product.getImagePublicIds() != null && product.getImageUrls() != null) {
            List<ImageInfo> imgs = new ArrayList<>();
            for (int i = 0; i < Math.min(product.getImagePublicIds().size(), product.getImageUrls().size()); i++) {
                imgs.add(new ImageInfo(product.getImagePublicIds().get(i), product.getImageUrls().get(i)));
            }
            r.setImages(imgs);
        } else {
            r.setImages(Collections.emptyList());
        }

        r.setAvailableColors(product.getAvailableColors() != null ? product.getAvailableColors() : Collections.emptyList());
        r.setAvailableSizes(product.getAvailableSizes() != null ? product.getAvailableSizes() : Collections.emptyList());

        // จัดการเรื่อง Variant Stocks
        if (product.getVariantStocks() != null) {
            List<VariantStockResponse> vs = product.getVariantStocks().stream().map(v -> {
                VariantStockResponse o = new VariantStockResponse();
                o.setColor(v.getColor());
                o.setSize(v.getSize());
                o.setQuantity(v.getQuantity());
                return o;
            }).collect(Collectors.toList());
            r.setVariantStocks(vs);
        } else {
            r.setVariantStocks(Collections.emptyList());
        }
        return r;
    }

    public ProductResponse updateProduct(
            String productId,
            ProductRequest productRequest,
            List<MultipartFile> newImages,
            List<String> removeImagePublicIds
    ) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // ฟิลด์ทั่วไป
        p.setName(productRequest.getName());
        p.setDescription(productRequest.getDescription());
        p.setPrice(productRequest.getPrice());
        p.setCategory(productRequest.getCategory());
        p.setAvailableColors(productRequest.getAvailableColors());
        p.setAvailableSizes(productRequest.getAvailableSizes());

        // ลบรูปตาม publicId
        if (removeImagePublicIds != null && !removeImagePublicIds.isEmpty()) {
            for (String pid : removeImagePublicIds) {
                cloudinaryService.deleteFile(pid);
            }
            // remove ออกจาก product ทั้งสองลิสต์
            if (p.getImagePublicIds() != null && p.getImageUrls() != null) {
                for (String pid : removeImagePublicIds) {
                    int idx = p.getImagePublicIds().indexOf(pid);
                    if (idx >= 0) {
                        p.getImagePublicIds().remove(idx);
                        if (idx < p.getImageUrls().size()) {
                            p.getImageUrls().remove(idx);
                        }
                    }
                }
            }
        }

        // อัปโหลดรูปใหม่
        if (newImages != null && !newImages.isEmpty()) {
            for (MultipartFile img : newImages) {
                CloudinaryUploadResponse resp = cloudinaryService.uploadFile(img, "products");
                p.getImagePublicIds().add(resp.getPublicId());
                p.getImageUrls().add(resp.getUrl());
            }
        }

        // อัปเดต variantStocks และสรุป stockQuantity
        if (productRequest.getVariantStocks() != null) {
            List<VariantStock> variants = productRequest.getVariantStocks().stream().map(v -> {
                VariantStock vs = new VariantStock();
                vs.setColor(v.getColor());
                vs.setSize(v.getSize());
                vs.setQuantity(v.getQuantity());
                return vs;
            }).collect(Collectors.toList());
            p.setVariantStocks(variants);
            int sum = variants.stream().mapToInt(VariantStock::getQuantity).sum();
            p.setStockQuantity(sum);
        } else {
            // fallback เก่า
            p.setStockQuantity(productRequest.getStockQuantity());
        }

        p.setUpdatedAt(LocalDateTime.now());
        Product saved = productRepository.save(p);
        return mapToProductResponse(saved);
    }

}
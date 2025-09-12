package com.shirtshop.service;


import com.shirtshop.dto.ProductRequest;
import com.shirtshop.dto.ProductResponse;
import com.shirtshop.entity.Product;
import com.shirtshop.exception.ResourceNotFoundException;
import com.shirtshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.shirtshop.dto.CloudinaryUploadResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService; // Inject Service สำหรับอัปโหลด

    /**
     * สร้างสินค้าใหม่ พร้อมอัปโหลดรูปภาพ
     * @param productRequest ข้อมูลสินค้าที่รับมาจาก client
     * @param images รายการไฟล์รูปภาพ
     * @return ProductResponse ข้อมูลสินค้าที่สร้างเสร็จแล้ว
     */
    public ProductResponse createProduct(ProductRequest productRequest, List<MultipartFile> images) {
        List<String> imageUrls = new ArrayList<>();
        List<String> imagePublicIds = new ArrayList<>(); // ⭐️ เพิ่ม List นี้

        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                // ⭐️ 1. รับค่าเป็น CloudinaryUploadResponse
                CloudinaryUploadResponse response = cloudinaryService.uploadFile(image, "products");
                // ⭐️ 2. ดึงค่า url และ publicId จาก object response
                imageUrls.add(response.getUrl());
                imagePublicIds.add(response.getPublicId());
            }
        }

        Product product = new Product();
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setCategory(productRequest.getCategory());
        product.setAvailableColors(productRequest.getAvailableColors());
        product.setAvailableSizes(productRequest.getAvailableSizes());
        product.setStockQuantity(productRequest.getStockQuantity());
        product.setImageUrls(imageUrls);
        product.setImagePublicIds(imagePublicIds); // ⭐️ 3. เซ็ตค่า public IDs
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        return mapToProductResponse(savedProduct);
    }

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

    /**
     * Helper method สำหรับแปลง Product (Entity) เป็น ProductResponse (DTO)
     * @param product object จากฐานข้อมูล
     * @return ProductResponse object สำหรับส่งกลับไปที่ API
     */
    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory());
        response.setImageUrls(product.getImageUrls());
        response.setAvailableColors(product.getAvailableColors());
        response.setAvailableSizes(product.getAvailableSizes());
        response.setStockQuantity(product.getStockQuantity());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }

    public ProductResponse getProductById(String id) {
        // ใช้ findById จาก Repository ซึ่งจะคืนค่าเป็น Optional<Product>
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // ถ้าเจอ product ให้แปลงเป็น DTO แล้วส่งกลับไป
        return mapToProductResponse(product);
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
}
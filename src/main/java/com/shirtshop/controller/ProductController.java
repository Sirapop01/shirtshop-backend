package com.shirtshop.controller;

import com.shirtshop.dto.ProductRequest;
import com.shirtshop.dto.ProductResponse;
import com.shirtshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Endpoint สำหรับสร้างสินค้าใหม่
     * รับข้อมูลแบบ multipart/form-data ซึ่งประกอบด้วย
     * - ส่วนของข้อมูลสินค้า (JSON)
     * - ส่วนของไฟล์รูปภาพ
     */
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") ProductRequest productRequest,
            @RequestPart("images") List<MultipartFile> images) {

        // ส่งต่อข้อมูลและไฟล์ทั้งหมดไปให้ ProductService จัดการ
        ProductResponse createdProduct = productService.createProduct(productRequest, images);

        // ส่ง Response กลับไปพร้อมสถานะ 201 Created
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products); // ส่ง Response กลับไปพร้อมสถานะ 200 OK
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ตัวอย่างการป้องกัน: ให้ลบได้เฉพาะ Admin
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // คืนค่า 204 No Content
    }
}
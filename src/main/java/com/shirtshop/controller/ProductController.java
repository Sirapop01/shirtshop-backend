package com.shirtshop.controller;

import com.shirtshop.dto.ProductRequest;
import com.shirtshop.dto.ProductResponse;
import com.shirtshop.entity.Product;
import com.shirtshop.mapper.ProductMapper;
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

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("q") String query) {
        List<Product> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        Product product = productService.getById(id);   // 👈 ให้ service return Entity
        ProductResponse response = ProductMapper.toResponse(product); // 👈 map เป็น DTO
        return ResponseEntity.ok(response);
    }



    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ตัวอย่างการป้องกัน: ให้ลบได้เฉพาะ Admin
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // คืนค่า 204 No Content
    }

    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @RequestPart("product") ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestPart(value = "removeImagePublicIds", required = false) List<String> removeImagePublicIds
    ) {
        ProductResponse updated = productService.updateProduct(id, productRequest, newImages, removeImagePublicIds);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/category/{categoryName}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String categoryName) {
        List<Product> products = productService.findByCategory(categoryName);
        return ResponseEntity.ok(products);
    }

}
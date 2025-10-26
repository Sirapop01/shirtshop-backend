package com.shirtshop.controller;

import com.shirtshop.dto.ProductRequest;
import com.shirtshop.dto.ProductResponse;
import com.shirtshop.dto.TopProductResponse;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") ProductRequest productRequest,
            @RequestPart("images") List<MultipartFile> images) {

        ProductResponse createdProduct = productService.createProduct(productRequest, images);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("q") String query) {
        List<Product> products = productService.searchProducts(query);
        return ResponseEntity.ok(products);
    }

    /** ✅ ใหม่: Top products สำหรับ Dashboard (กันชนพาธ /{id}) */
    @GetMapping("/top")
    public ResponseEntity<List<TopProductResponse>> getTopProducts(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "today") String range
    ) {
        return ResponseEntity.ok(productService.getTopProducts(limit, range));
    }

    /** ✅ จำกัดพาธให้รับเฉพาะ ObjectId 24 ตัวฐานสิบหก */
    @GetMapping("/{id:[a-f0-9]{24}}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        Product product = productService.getById(id);
        ProductResponse response = ProductMapper.toResponse(product);
        return ResponseEntity.ok(response);
    }

    /** ✅ จำกัดพาธให้รับเฉพาะ ObjectId 24 ตัวฐานสิบหก */
    @DeleteMapping("/{id:[a-f0-9]{24}}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /** ✅ จำกัดพาธให้รับเฉพาะ ObjectId 24 ตัวฐานสิบหก */
    @PutMapping(value = "/{id:[a-f0-9]{24}}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
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

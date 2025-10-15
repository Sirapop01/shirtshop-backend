package com.shirtshop.service;

import com.shirtshop.dto.*;
import com.shirtshop.entity.Product;
import com.shirtshop.entity.VariantStock;
import com.shirtshop.exception.ResourceNotFoundException;
import com.shirtshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService; // Inject Service สำหรับอัปโหลด

    /**
     * ดึงข้อมูลสินค้าทั้งหมด
     */
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    public Product getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByNameContainingIgnoreCase(query);
    }

    public void deleteProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getImagePublicIds() != null && !product.getImagePublicIds().isEmpty()) {
            for (String publicId : product.getImagePublicIds()) {
                cloudinaryService.deleteFile(publicId);
            }
        }
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

        // รูปภาพ
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

        // Variant Stocks
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
                ensureList(p);
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
            p.setStockQuantity(productRequest.getStockQuantity());
        }

        p.setUpdatedAt(LocalDateTime.now());
        Product saved = productRepository.save(p);
        return mapToProductResponse(saved);
    }

    public List<Product> findByCategory(String categoryName) {
        return productRepository.findByCategoryIgnoreCase(categoryName);
    }

    /** ✅ ใช้ใน Dashboard: Top products */
    public List<TopProductResponse> getTopProducts(int limit, String range) {
        // TODO: ถ้ามี OrderRepository ให้ aggregate ยอดขายจริงตามช่วงเวลา (range)
        // Fallback ชั่วคราว: ยังไม่มีข้อมูลยอดขาย -> units = 0, revenue = 0
        // และเรียงจาก stockQuantity มาก -> น้อย เพื่อให้มีรายการขึ้นหน้าจอ

        List<Product> all = productRepository.findAll();

        return all.stream()
                .sorted(Comparator
                        .comparing(Product::getStockQuantity, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(p -> Optional.ofNullable(p.getUpdatedAt()).orElse(LocalDateTime.MIN), Comparator.reverseOrder()))
                .limit(Math.max(1, limit))
                .map(p -> TopProductResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .units(0L) // ยังไม่เชื่อมออเดอร์ -> 0 ไปก่อน
                        .revenue(BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    // ---------- helpers ----------
    private void ensureList(Product p) {
        if (p.getImagePublicIds() == null) p.setImagePublicIds(new ArrayList<>());
        if (p.getImageUrls() == null) p.setImageUrls(new ArrayList<>());
    }
}

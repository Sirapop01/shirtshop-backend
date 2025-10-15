package com.shirtshop.controller;

import com.shirtshop.dto.CustomerItemResponse;
import com.shirtshop.dto.UserDetailResponse;
import com.shirtshop.service.CustomerAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerAdminController {

    private final CustomerAdminService customerAdminService;

    /** ดึงลูกค้าทั้งหมด */
    @GetMapping
    public ResponseEntity<List<CustomerItemResponse>> listAll() {
        return ResponseEntity.ok(customerAdminService.getAllCustomers());
    }

    /** นับจำนวนลูกค้า (ใช้ใน Dashboard) */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        long total = customerAdminService.countAll(); // ให้มีเมธอดนี้ใน service (repo.count())
        // ใส่ทั้ง "total" และ "count" เผื่อ frontend อิง key ใด key หนึ่ง
        return ResponseEntity.ok(Map.of("total", total, "count", total));
    }

    /** ดูรายละเอียดโปรไฟล์ลูกค้า (รับเฉพาะ Mongo ObjectId 24 ตัวฐานสิบหก) */
    @GetMapping("/{id:[a-f0-9]{24}}")
    public ResponseEntity<UserDetailResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerAdminService.getDetailById(id));
    }

    /** ลบลูกค้า (รับเฉพาะ Mongo ObjectId) */
    @DeleteMapping("/{id:[a-f0-9]{24}}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        customerAdminService.deleteCustomerById(id);
        return ResponseEntity.noContent().build();
    }
}

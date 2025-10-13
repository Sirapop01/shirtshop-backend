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

    /**
     * ดึงลูกค้าทั้งหมด
     */
    @GetMapping
    public ResponseEntity<List<CustomerItemResponse>> listAll() {
        return ResponseEntity.ok(customerAdminService.getAllCustomers());
    }

    /**
     * ดูรายละเอียดโปรไฟล์ลูกค้า
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(customerAdminService.getDetailById(id)); // ✅ แก้ชื่อให้ตรง
    }

    /**
     * ลบลูกค้า
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        customerAdminService.deleteCustomerById(id); // ✅ แก้ชื่อให้ตรง
        return ResponseEntity.noContent().build();
    }
}

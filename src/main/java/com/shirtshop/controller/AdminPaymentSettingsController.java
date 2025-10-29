// src/main/java/com/shirtshop/controller/AdminPaymentSettingsController.java
package com.shirtshop.controller;

import com.shirtshop.entity.PaymentSettings;
import com.shirtshop.service.PaymentSettingsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings/payment")
@RequiredArgsConstructor
public class AdminPaymentSettingsController {

    private final PaymentSettingsService service;

    @GetMapping("/promptpay")
    public ResponseEntity<PaymentSettings> get() {
        return ResponseEntity.ok(service.getOrInit());
    }

    @PutMapping("/promptpay")
    public ResponseEntity<PaymentSettings> update(@RequestBody UpdateReq req) {
        return ResponseEntity.ok(service.update(req.getTarget(), req.getExpireMinutes()));
    }

    @Data
    public static class UpdateReq {
        private String target;       // เบอร์/พร้อมเพย์ไอดี
        private Integer expireMinutes;
    }
}

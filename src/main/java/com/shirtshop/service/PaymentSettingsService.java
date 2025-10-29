// src/main/java/com/shirtshop/service/PaymentSettingsService.java
package com.shirtshop.service;

import com.shirtshop.entity.PaymentSettings;
import com.shirtshop.repository.PaymentSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentSettingsService {

    private final PaymentSettingsRepository repo;

    @Value("${app.payment.promptpay.target:}")
    private String defaultTarget;

    @Value("${app.payment.promptpay.expire-minutes:1}")
    private int defaultExpireMinutes;

    /** ดึง หรือสร้างด้วย default */
    public PaymentSettings getOrInit() {
        return repo.findById("PAYMENT_PROMPTPAY").orElseGet(() -> {
            PaymentSettings s = new PaymentSettings();
            s.setTarget(defaultTarget);
            s.setExpireMinutes(defaultExpireMinutes);
            return repo.save(s);
        });
    }

    /** อัปเดตค่าพร้อม validate ง่าย ๆ */
    public PaymentSettings update(String target, Integer expireMinutes) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("PromptPay target is required");
        }
        if (expireMinutes == null || expireMinutes < 1 || expireMinutes > 60*24) {
            throw new IllegalArgumentException("expireMinutes must be 1..1440");
        }
        // ถ้าเป็นเบอร์ไทย รูปแบบคร่าวๆ (ไม่บังคับเคร่ง)
        if (target.matches("^\\d+$") && !target.matches("^0\\d{8,9}$")) {
            throw new IllegalArgumentException("Invalid Thai phone number format");
        }

        PaymentSettings s = getOrInit();
        s.setTarget(target.trim());
        s.setExpireMinutes(expireMinutes);
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }
}

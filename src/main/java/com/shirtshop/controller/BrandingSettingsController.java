// src/main/java/com/shirtshop/controller/BrandingSettingsController.java
package com.shirtshop.controller;

import com.shirtshop.dto.BrandingResponse;
import com.shirtshop.dto.BrandingUpdateRequest;
import com.shirtshop.service.BrandingSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class BrandingSettingsController {

    private final BrandingSettingsService service;

    @GetMapping("/branding")
    public BrandingResponse getBranding() {
        return service.getBranding();
    }

    // ✅ รับ text ด้วย @RequestParam, รับไฟล์ด้วย @RequestPart(logo) เท่านั้น
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/branding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BrandingResponse updateBrandingPut(
            @RequestParam(value = "siteName",   required = false) String siteName,
            @RequestParam(value = "removeLogo", required = false) String removeLogoStr,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        BrandingUpdateRequest req = new BrandingUpdateRequest();
        if (siteName != null && !siteName.isBlank()) req.setSiteName(siteName);

        if (removeLogoStr != null) {
            String v = removeLogoStr.trim().toLowerCase();
            boolean remove = v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
            req.setRemoveLogo(remove);
        }
        return service.updateBranding(req, logo);
    }

    // (จะมี/ไม่มีก็ได้ ถ้าใช้ PUT อย่างเดียว)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/branding", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BrandingResponse updateBrandingPost(
            @RequestParam(value = "siteName",   required = false) String siteName,
            @RequestParam(value = "removeLogo", required = false) String removeLogoStr,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        BrandingUpdateRequest req = new BrandingUpdateRequest();
        if (siteName != null && !siteName.isBlank()) req.setSiteName(siteName);

        if (removeLogoStr != null) {
            String v = removeLogoStr.trim().toLowerCase();
            boolean remove = v.equals("true") || v.equals("1") || v.equals("on") || v.equals("yes");
            req.setRemoveLogo(remove);
        }
        return service.updateBranding(req, logo);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/branding/logo")
    public void deleteLogo() {
        service.deleteLogo();
    }

    @GetMapping("/branding/ping")
    public java.util.Map<String, String> ping() {
        return java.util.Map.of("ok", "true");
    }
}

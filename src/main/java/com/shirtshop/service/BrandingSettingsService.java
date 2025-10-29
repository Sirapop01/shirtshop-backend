// src/main/java/com/shirtshop/service/BrandingSettingsService.java
package com.shirtshop.service;

import com.shirtshop.dto.BrandingResponse;
import com.shirtshop.dto.BrandingUpdateRequest;
import com.shirtshop.dto.CloudinaryUploadResponse;
import com.shirtshop.entity.BrandingSettings;
import com.shirtshop.repository.BrandingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BrandingSettingsService {

    private final BrandingSettingsRepository repo;
    private final CloudinaryService cloudinaryService;

    private static final String BRANDING_ID = "branding";
    private static final String BRANDING_FOLDER = "shirtshop/branding";

    public BrandingResponse getBranding() {
        BrandingSettings s = repo.findById(BRANDING_ID).orElseGet(() -> {
            BrandingSettings d = new BrandingSettings();
            d.setId(BRANDING_ID);
            d.setSiteName("StyleWhere");
            return repo.save(d);
        });
        return new BrandingResponse(s.getSiteName(), s.getLogoUrl());
    }

    public BrandingResponse updateBranding(BrandingUpdateRequest req, MultipartFile logo) {
        BrandingSettings s = repo.findById(BRANDING_ID).orElseGet(() -> {
            BrandingSettings d = new BrandingSettings();
            d.setId(BRANDING_ID);
            return d;
        });

        if (req.getSiteName() != null) {
            s.setSiteName(req.getSiteName().trim());
        }

        if (Boolean.TRUE.equals(req.getRemoveLogo())) {
            if (s.getLogoPublicId() != null) {
                try { cloudinaryService.deleteFile(s.getLogoPublicId()); } catch (Exception ex) {
                    // log แล้วไปต่อ (อย่าทำให้ทั้ง flow พัง)
                    System.err.println("[Branding] delete old logo failed: " + ex.getMessage());
                }
            }
            s.setLogoPublicId(null);
            s.setLogoUrl(null);
        }

        if (logo != null && !logo.isEmpty()) {
            // เคลียร์ของเดิม
            if (s.getLogoPublicId() != null) {
                try { cloudinaryService.deleteFile(s.getLogoPublicId()); } catch (Exception ex) {
                    System.err.println("[Branding] delete old logo before upload failed: " + ex.getMessage());
                }
            }

            try {
                // ถ้า DTO ของคุณมีแค่ publicId + url
                var res = cloudinaryService.uploadFile(logo, "shirtshop/branding");
                if (res == null) {
                    throw new IllegalStateException("Cloudinary upload returned null");
                }
                String publicId = res.getPublicId();
                String url = res.getUrl(); // ไม่มี secureUrl ก็ใช้ url

                if (url == null || url.isBlank()) {
                    throw new IllegalStateException("Cloudinary upload has empty url");
                }

                s.setLogoPublicId(publicId);
                s.setLogoUrl(url);
            } catch (Exception ex) {
                // โยนข้อความที่อ่านรู้เรื่องกลับให้ FE (500 อยู่ แต่มี message)
                System.err.println("[Branding] upload failed: " + ex.getMessage());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Upload logo failed: " + ex.getMessage()
                );
            }
        }

        repo.save(s);
        return new BrandingResponse(s.getSiteName(), s.getLogoUrl());
    }


    public void deleteLogo() {
        BrandingSettings s = repo.findById(BRANDING_ID).orElseThrow();
        if (s.getLogoPublicId() != null) {
            try { cloudinaryService.deleteFile(s.getLogoPublicId()); } catch (Exception ignored) {}
        }
        s.setLogoPublicId(null);
        s.setLogoUrl(null);
        repo.save(s);
    }
}

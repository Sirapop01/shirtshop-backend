// src/main/java/com/shirtshop/entity/BrandingSettings.java
package com.shirtshop.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "settings")
public class BrandingSettings {
    @Id
    private String id;               // แนะนำให้ใช้ค่าคงที่ "branding"
    private String siteName;

    // เก็บข้อมูลโลโก้ (แยก url / publicId เพื่อรองรับลบไฟล์บน Cloudinary)
    private String logoUrl;
    private String logoPublicId;
}

package com.shirtshop.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "addresses")
public class Address {
    @Id
    private String id;

    private String userId;        // อ้างอิงผู้ใช้ที่ล็อกอิน
    private String fullName;
    private String phone;
    private String addressLine1;
    private String subdistrict;   // ชื่อตำบล/แขวง
    private String district;      // amphure_id (string)
    private String province;      // province_id (string)
    private String postalCode;
    private boolean isDefault;
}

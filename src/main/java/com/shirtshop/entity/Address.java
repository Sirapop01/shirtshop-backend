// src/main/java/com/shirtshop/entity/Address.java
package com.shirtshop.entity;

import lombok.*;
import org.springframework.data.annotation.Id;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Address {
    @Id
    private String id;

    private String fullName;       // ชื่อ-นามสกุลผู้รับ
    private String phone;          // เบอร์
    private String addressLine1;   // บ้านเลขที่/ซอย/ถนน
    private String subdistrict;    // ตำบล/แขวง
    private String district;       // อำเภอ/เขต
    private String province;       // จังหวัด
    private String postalCode;     // 5 หลัก

    private boolean isDefault;     // true ได้แค่ 1 รายการต่อ user
}

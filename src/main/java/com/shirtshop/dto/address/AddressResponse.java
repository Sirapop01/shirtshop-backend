// src/main/java/com/shirtshop/dto/address/AddressResponse.java
package com.shirtshop.dto.address;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shirtshop.entity.Address;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressResponse {
    private String id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String subdistrict;

    private String district;      // เดิม: อาจเป็นรหัส "1101"
    private String province;      // เดิม: อาจเป็นรหัส "2"
    private String postalCode;
    private boolean isDefault;

    // ✅ เพิ่มฟิลด์ชื่ออ่านง่าย
    private String districtName;
    private String provinceName;

    public static AddressResponse fromEntity(Address a) {
        return AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .addressLine1(a.getAddressLine1())
                .subdistrict(a.getSubdistrict())
                .district(a.getDistrict())
                .province(a.getProvince())
                .postalCode(a.getPostalCode())
                .isDefault(a.isDefault())
                .districtName(a.getDistrictName())
                .provinceName(a.getProvinceName())
                .build();
    }
}

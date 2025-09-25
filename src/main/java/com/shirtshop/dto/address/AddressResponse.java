package com.shirtshop.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shirtshop.entity.Address;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddressResponse {
    private String id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String subdistrict;
    private String district;
    private String province;
    private String postalCode;
    @JsonProperty("isDefault")
    private boolean isDefault;


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
                .build();
    }
}

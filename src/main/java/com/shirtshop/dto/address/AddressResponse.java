// src/main/java/com/shirtshop/dto/address/AddressResponse.java
package com.shirtshop.dto.address;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressResponse {
    private String id;
    private String fullName;
    private String phone;
    private String addressLine1;
    private String subdistrict;
    private String district;
    private String province;
    private String postalCode;
    private boolean isDefault;
}

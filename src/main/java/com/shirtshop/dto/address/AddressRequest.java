package com.shirtshop.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {
    @NotBlank
    private String fullName;

    @Pattern(regexp = "^0\\d{8,9}$", message = "เบอร์โทรไม่ถูกต้อง")
    private String phone;

    @NotBlank
    private String addressLine1;

    @NotBlank
    private String subdistrict;

    @NotBlank
    private String district; // amphure_id

    @NotBlank
    private String province; // province_id

    @Pattern(regexp = "^\\d{5}$", message = "รหัสไปรษณีย์ไม่ถูกต้อง")
    private String postalCode;

    @JsonProperty("isDefault")
    private boolean isDefault;
}

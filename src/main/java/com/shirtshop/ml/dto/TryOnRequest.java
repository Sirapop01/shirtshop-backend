package com.shirtshop.ml.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TryOnRequest {
    /**
     * base64 ของภาพคน (เช่น data:image/jpeg;base64,<...> หรือจะส่งเฉพาะ payload ก็ได้)
     */
    @NotBlank(message = "personImageBase64 is required")
    private String personImageBase64;

    /**
     * base64 ของภาพเสื้อ (front / upper-body)
     */
    @NotBlank(message = "garmentImageBase64 is required")
    private String garmentImageBase64;

    /**
     * ตัวเลือกเสริมต่าง ๆ เช่น size, prompt, seed ฯลฯ (optional)
     */
    private String optionsJson;
}

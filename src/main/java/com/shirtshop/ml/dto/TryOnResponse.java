package com.shirtshop.ml.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TryOnResponse {
    /**
     * รูปผลลัพธ์ที่ได้จาก Gradio (base64 string; ถ้าฝั่ง Gradio ส่ง URL กลับมาก็เปลี่ยนเป็น resultUrl ได้)
     */
    private String resultBase64;

    /**
     * เวลาโดยรวมที่ใช้ (เชิงสถิติ)
     */
    private Long elapsedMs;

    /**
     * ข้อความเพิ่มเติมจากฝั่ง ML (optional)
     */
    private String message;
}

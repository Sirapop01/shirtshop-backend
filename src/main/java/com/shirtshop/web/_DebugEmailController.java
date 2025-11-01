// src/main/java/com/shirtshop/web/_DebugEmailController.java
package com.shirtshop.web;

import com.shirtshop.util.EmailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class _DebugEmailController {
    private final EmailSender sender;
    public _DebugEmailController(EmailSender sender) { this.sender = sender; }

    @GetMapping("/api/_debug/email-sender")
    public String which() {
        return sender.getClass().getName();  // อย่าลืมลบไฟล์นี้หลังดีบักเสร็จ
    }
}

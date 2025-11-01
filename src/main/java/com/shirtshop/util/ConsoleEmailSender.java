package com.shirtshop.util;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!smtp & !resend") // โหลดเมื่อไม่ได้เปิด smtp และไม่ได้เปิด resend
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String html) {
        System.out.println("=== SEND EMAIL (Console) ===");
        System.out.println("To: " + to);
        System.out.println("Subject: " + subject);
        System.out.println("HTML: " + html);
        System.out.println("============================");
    }
}

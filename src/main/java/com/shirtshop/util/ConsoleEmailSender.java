package com.shirtshop.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service // <-- ทำให้เป็น Spring bean
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        // เปลี่ยนไปเป็น SMTP จริงภายหลังได้
        log.info("\n=== SEND EMAIL ===\nTo: {}\nSubject: {}\nBody:\n{}\n==================\n",
                to, subject, body);
    }
}

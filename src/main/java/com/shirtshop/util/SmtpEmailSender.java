package com.shirtshop.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
@Profile("smtp") // ใช้เฉพาะเมื่อเปิดโปรไฟล์ smtp
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Override
    public void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress("no-reply@example.com")); // หรือใช้ from จาก config ถ้าผูกไว้
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setContent(html, "text/html; charset=UTF-8");
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send SMTP mail", e);
        }
    }
}

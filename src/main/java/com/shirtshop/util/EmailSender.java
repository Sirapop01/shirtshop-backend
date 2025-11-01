package com.shirtshop.util;

public interface EmailSender {
    void send(String to, String subject, String html);
}

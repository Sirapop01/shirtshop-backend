package com.shirtshop.util;

import java.time.Duration;

public interface OtpStore {
    void save(String email, String otp, Duration ttl);
    boolean verify(String email, String otp);
    void consume(String email);
}

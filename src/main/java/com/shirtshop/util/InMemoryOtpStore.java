package com.shirtshop.util;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryOtpStore implements OtpStore {

    private static class Entry {
        final String otp;
        final Instant expiresAt;
        Entry(String otp, Instant expiresAt) {
            this.otp = otp; this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String email, String otp, Duration ttl) {
        store.put(email, new Entry(otp, Instant.now().plus(ttl)));
    }

    @Override
    public boolean verify(String email, String otp) {
        Entry e = store.get(email);
        if (e == null) return false;
        if (Instant.now().isAfter(e.expiresAt)) {
            store.remove(email);
            return false;
        }
        return e.otp.equals(otp);
    }

    @Override
    public void consume(String email) {
        store.remove(email);
    }
}

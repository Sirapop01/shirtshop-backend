package com.shirtshop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentUser {
    public static String idOrThrow() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) throw new RuntimeException("Unauthenticated");
        return a.getName(); // เราใส่ userId เป็น principal ตอนตรวจ JWT
    }
}

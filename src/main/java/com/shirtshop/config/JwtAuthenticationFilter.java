package com.shirtshop.config;

import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import com.shirtshop.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // ถ้าไม่มี header ก็ให้ข้ามไป
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ตัดคำว่า Bearer ออก
        String token = authHeader.substring(7);

        // ตรวจสอบความถูกต้องของ token
        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ดึง userId จาก token
        String userId = jwtService.extractUserId(token);

        // โหลดข้อมูล User จริงจากฐานข้อมูล
        User user = userRepository.findById(userId).orElse(null);

        if (user != null) {
            // ใส่ User object เป็น principal
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // ให้ request ผ่านต่อ
        filterChain.doFilter(request, response);
    }
}

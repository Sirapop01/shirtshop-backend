package com.shirtshop.security;

import com.shirtshop.user.User;
import com.shirtshop.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwt;

    @Value("${app.oauth2.redirect-success}")
    private String redirectSuccess;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        var principal = token.getPrincipal();
        String email = (String) principal.getAttributes().get("email");

        User user = userRepository.findByEmail(email).orElseThrow();
        String roles = String.join(",", user.getRoles().stream().map(Enum::name).toList());

        String access  = jwt.genAccess(user.getId(), user.getEmail(), roles);
        String refresh = jwt.genRefresh(user.getId());

        String url = UriComponentsBuilder.fromUriString(redirectSuccess)
                .fragment("access=" + access + "&refresh=" + refresh)
                .build(true).toUriString();

        response.sendRedirect(url);
    }
}

package com.shirtshop.service;

import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        var delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(req);

        String provider = req.getClientRegistration().getRegistrationId(); // google/facebook
        Map<String, Object> attrs = oauth2User.getAttributes();

        String email;
        String providerId;

        if ("google".equals(provider)) {
            email = (String) attrs.get("email");
            providerId = (String) attrs.get("sub");
        } else if ("facebook".equals(provider)) {
            email = (String) attrs.get("email");
            providerId = (String) attrs.get("id");
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseGet(() -> User.builder()
                        .email(email.toLowerCase())
                        .displayName((String) attrs.getOrDefault("name", email))
                        .emailVerified(true)
                        .build());

        user.setAuthProvider(provider.toUpperCase());
        user.setProviderId(providerId);
        userRepository.save(user);

        return new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                attrs,
                "sub"
        );
    }
}

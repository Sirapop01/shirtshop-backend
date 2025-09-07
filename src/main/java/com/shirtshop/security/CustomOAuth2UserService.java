package com.shirtshop.security;

import com.shirtshop.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        var delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(req);

        String regId = req.getClientRegistration().getRegistrationId(); // "google"|"facebook"
        Map<String, Object> attr = oAuth2User.getAttributes();

        String email   = extractEmail(regId, attr);
        String name    = extractName(regId, attr);
        String sub     = extractProviderId(regId, attr);
        String picture = extractAvatar(regId, attr);

        if (email == null || email.isBlank()) {
            OAuth2Error error = new OAuth2Error(
                    "email_missing",
                    "Email not provided by provider",
                    null
            );
            throw new OAuth2AuthenticationException(error);
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .email(email)
                .displayName(name)
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());

        user.setDisplayName(name != null ? name : user.getDisplayName());
        user.setProvider("google".equals(regId) ? AuthProvider.GOOGLE : AuthProvider.FACEBOOK);
        user.setProviderId(sub);
        if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())) {
            user.setAvatarUrl(picture);
        }

        userRepository.save(user);
        // ให้ Spring จัดการ principal เดิมต่อได้ เราจะออก JWT ใน SuccessHandler
        return oAuth2User;
    }

    private String extractEmail(String regId, Map<String,Object> a){
        if ("google".equals(regId))   return (String) a.get("email");
        if ("facebook".equals(regId)) return (String) a.get("email");
        return null;
    }

    private String extractName(String regId, Map<String,Object> a){
        if ("google".equals(regId))   return (String) a.get("name");
        if ("facebook".equals(regId)) return (String) a.get("name");
        return null;
    }

    private String extractProviderId(String regId, Map<String,Object> a){
        if ("google".equals(regId))   return (String) a.get("sub");
        if ("facebook".equals(regId)) return String.valueOf(a.get("id"));
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractAvatar(String regId, Map<String,Object> a){
        if ("google".equals(regId)) return (String) a.get("picture");
        if ("facebook".equals(regId)) {
            Object pic = a.get("picture");
            if (pic instanceof Map<?,?> m) {
                Object data = m.get("data");
                if (data instanceof Map<?,?> d) return Objects.toString(d.get("url"), null);
            }
        }
        return null;
    }
}

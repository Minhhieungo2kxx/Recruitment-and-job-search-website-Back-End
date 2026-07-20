package com.webjob.application.service.OAuth2;

import com.webjob.application.enums.UserStatus;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.RoleRepository;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String email = oauthUser.getAttribute("email");
        Map<String, Object> attributes = new HashMap<>(oauthUser.getAttributes());
        User dbUser = userService.getEmailbyGoogle(email);

        if (dbUser != null) {

            if (dbUser.isDeleted()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_deleted"),
                        "Account has been deleted."
                );
            }

            switch (dbUser.getStatus()) {
                case BLOCKED:
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("account_blocked"),
                            "Account has been blocked."
                    );

                case INACTIVE:
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("account_inactive"),
                            "Account is inactive."
                    );

                case PENDING:
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error("account_pending"),
                            "Account is pending verification."
                    );

                case ACTIVE:
                    break;
            }

            attributes.put("userId", dbUser.getId().toString());
        }
        else{
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(oauthUser.getAttribute("name"));
            newUser.setAvatar("https://static.vecteezy.com/system/resources/thumbnails/009/292/244/small/default-avatar-icon-of-social-media-user-vector.jpg");
            newUser.setPassword(passwordEncoder.encode("OAUTH2_"+UUID.randomUUID().toString().substring(0, 8)));
            int age = ThreadLocalRandom.current().nextInt(18, 61);
            LocalDate birthDate = LocalDate.now().minusYears(age)
                    .minusDays(ThreadLocalRandom.current().nextInt(365));
            newUser.setDateOfBirth(birthDate);

            newUser.setGender("MALE");
            newUser.setStatus(UserStatus.ACTIVE);
            newUser.setDeleted(false);
            newUser.setAddress("No Information");
            newUser.setPhone("No information");
            Role role = roleRepository.findByCodeAndActiveTrue("USER")
                    .orElseThrow(() -> new RuntimeException("Default role USER not found"));

            newUser.setRole(role);
            newUser.setCompany(null);
            User save= userRepository.save(newUser);
            attributes.put("userId", save.getId().toString());
        }

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "userId"   //  QUAN TRỌNG
        );
    }

}

package com.webjob.application.Service.OAuth2;

import com.webjob.application.Model.Entity.Role;
import com.webjob.application.Model.Entity.User;
import com.webjob.application.Repository.RoleRepository;
import com.webjob.application.Repository.UserRepository;
import com.webjob.application.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        if(dbUser!=null){
            attributes.put("userId", dbUser.getId().toString());
        }
        else{
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(oauthUser.getAttribute("name"));
            newUser.setAvatar("default.png");
            newUser.setPassword(passwordEncoder.encode("OAUTH2_"+UUID.randomUUID().toString().substring(0, 8)));
            newUser.setAge(18);
            newUser.setGender("MALE");
            newUser.setAddress("Unknown Address");
            Role role=roleRepository.findByName("USER");
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

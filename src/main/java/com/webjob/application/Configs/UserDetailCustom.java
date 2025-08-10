package com.webjob.application.Configs;

import com.webjob.application.Services.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.User;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


@Component("userDetailsService")
public class UserDetailCustom implements UserDetailsService {
    private final UserService userService;

    public UserDetailCustom(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.webjob.application.Models.Entity.User user=userService.getbyEmail(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found "+username);
        }

        return new User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_"+user.getRole().getName().trim()))); // Chuyển đổi role
        // thành
        // authority


    }
}

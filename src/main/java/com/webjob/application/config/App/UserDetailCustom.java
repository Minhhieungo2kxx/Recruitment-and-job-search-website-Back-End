package com.webjob.application.config.App;

import com.webjob.application.enums.UserStatus;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.service.UserService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

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
        com.webjob.application.models.Entity.User user=userService.getbyEmail(username);
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UnauthorizedException("Account has been blocked");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new UnauthorizedException("Account is pending verification");
        }

        return new CustomUserDetails(
                user.getId(),                 // userId
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().getCode().trim()
                        )
                )
        );


    }
}

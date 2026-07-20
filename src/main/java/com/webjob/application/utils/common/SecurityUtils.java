package com.webjob.application.utils.common;

import com.webjob.application.enums.CompanyStatus;
import com.webjob.application.exception.Customs.ForbiddenException;
import com.webjob.application.exception.Customs.ResourceNotFoundException;
import com.webjob.application.exception.Customs.UnauthorizedException;
import com.webjob.application.models.Entity.Company;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = Long.valueOf(authentication.getName());
        User user=userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or no Active  with id: " + userId));

        switch (user.getStatus()) {
            case ACTIVE:
                return user;

            case BLOCKED:
                throw new UnauthorizedException("Your account has been blocked.");

            case PENDING:
                throw new UnauthorizedException("Your account is pending verification.");

            case INACTIVE:
                throw new UnauthorizedException("Your account has been deactivated.");

            default:
                throw new UnauthorizedException("Invalid account status.");
        }

    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(authentication.getName());
    }


}

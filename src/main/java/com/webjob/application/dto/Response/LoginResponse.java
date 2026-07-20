package com.webjob.application.dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.webjob.application.enums.UserStatus;
import com.webjob.application.models.Entity.Permission;
import com.webjob.application.models.Entity.Role;
import com.webjob.application.models.Entity.RolePermission;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    @JsonProperty("access_token")
    private String accessToken;

    private User user;

    @JsonIgnore
    private String refreshCookie;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class User {
        private Long id;
        private String email;
        private String fullName;
        private String avatar;
        private LocalDate dateOfBirth;
        private UserStatus status;
        private Role role;


    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Role {
        private Long id;
        private String code; // HR_MANAGER
        private String displayName; // HR
        private List<RolePermission> rolePermissions;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RolePermission{
        private Long id;
        private Permission permission;

    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Permission{
        private Long id;
        private String name;

    }

}

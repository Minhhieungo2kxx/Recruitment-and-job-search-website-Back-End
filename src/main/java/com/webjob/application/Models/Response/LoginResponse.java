package com.webjob.application.Models.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.webjob.application.Models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @JsonProperty("access_token")
    private String accessToken;
    private User user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private Long id;
        private String email;
        private String fullName;
        private Role role;



    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserinsideToken {
        private Long id;
        private String email;
        private String username;
        

    }
}

package com.webjob.application.Models.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private User user;

    public LoginResponse(String accessToken, User user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User {
        private Long id;
        private String email;
        private String username;


    }
}

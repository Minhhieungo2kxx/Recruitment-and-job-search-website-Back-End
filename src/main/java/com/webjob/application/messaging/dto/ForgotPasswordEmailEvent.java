package com.webjob.application.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ForgotPasswordEmailEvent {
    private String email;

    private String fullName;

    private String token;

    private Instant expiresAt;
}

package com.webjob.application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPresenceDTO {
    private Long userId;
    private boolean isOnline;
    private Instant lastSeenAt;
    private String statusText; // "Đang hoạt động", "Vừa truy cập", "5 phút trước", v.v.
    private String statusType; // "online", "recently", "away", "offline"
}

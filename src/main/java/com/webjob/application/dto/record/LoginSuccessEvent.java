package com.webjob.application.dto.record;

import java.time.LocalDateTime;

public record LoginSuccessEvent(
        String email,
        String ip,
        String userAgent,
        LocalDateTime loginTime
) {
}

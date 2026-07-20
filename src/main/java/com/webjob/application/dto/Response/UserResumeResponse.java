package com.webjob.application.dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResumeResponse {
    private Long id;

    private String name;

    private String url;

    private Boolean isDefault;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd HH:mm:ss a z",
            timezone = "Asia/Ho_Chi_Minh",
            locale = "en_US"
    )
    private Instant createdAt;
}

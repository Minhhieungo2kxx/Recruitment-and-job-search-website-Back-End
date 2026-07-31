package com.webjob.application.dto.Response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowCompanyResponse {

    private Long companyId;

    private String companyName;

    private String logo;


    private boolean notificationEnabled;

    private Instant followedAt;


//    private boolean followed;
}

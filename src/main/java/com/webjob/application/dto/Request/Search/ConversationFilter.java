package com.webjob.application.dto.Request.Search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationFilter {

    private Long userId;
    private Instant startDate;
    private Instant endDate;
}

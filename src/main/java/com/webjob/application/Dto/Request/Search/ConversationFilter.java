package com.webjob.application.Dto.Request.Search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationFilter {
    private String page;
    private int size = 20;
    private Long userId;
    private Instant startDate;
    private Instant endDate;
}

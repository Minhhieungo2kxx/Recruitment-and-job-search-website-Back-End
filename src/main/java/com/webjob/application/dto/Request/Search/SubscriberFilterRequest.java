package com.webjob.application.dto.Request.Search;

import com.webjob.application.enums.SubscriberStatus;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SubscriberFilterRequest {
    private String keyword;

    private SubscriberStatus status;

    private List<Long> skillIds;

    private Instant fromDate;

    private Instant toDate;
}

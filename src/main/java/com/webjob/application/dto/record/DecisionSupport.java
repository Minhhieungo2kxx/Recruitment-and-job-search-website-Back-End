package com.webjob.application.dto.record;

import java.util.List;

public record DecisionSupport(
        List<String> interviewFocus,
        List<String> verificationItems,
        List<String> considerations
) {
}

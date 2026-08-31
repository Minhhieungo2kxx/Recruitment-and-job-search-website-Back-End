package com.webjob.application.dto.Request;

import com.webjob.application.document.JobDocument;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobRestoredEvent {
    private JobDocument document;
}

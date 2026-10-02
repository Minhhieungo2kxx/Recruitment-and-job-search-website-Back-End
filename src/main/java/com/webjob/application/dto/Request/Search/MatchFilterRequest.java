package com.webjob.application.dto.Request.Search;

import com.webjob.application.enums.MatchStatus;
import com.webjob.application.enums.ResumeStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MatchFilterRequest {
    private Double minScore;
    private Double maxScore;

    private MatchStatus matchStatus;


    private ResumeStatus resumeStatus;

}

package com.webjob.application.dto.Response;

import com.webjob.application.enums.OutcomeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoopOutcome {

    private OutcomeType type;
    private String finalText;
    private String pendingActionId;
    private String toolName;
    private Map<String, Object> preview;


    public static LoopOutcome finalText(String text) {
        return LoopOutcome.builder()
                .type(OutcomeType.FINAL_TEXT)
                .finalText(text)
                .build();
    }
}

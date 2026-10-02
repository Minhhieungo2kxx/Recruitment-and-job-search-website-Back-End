package com.webjob.application.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class MatchingScoringProperties {
    private Weights weights = new Weights();

    private double requiredSkillMissingGateRatio = 0.4;
    private double requiredSkillGatePenaltyFactor = 0.5;
    private double preferredSkillMaxBonus = 20;

    @Getter
    @Setter
    public static class Weights {
        private double skill = 0.50;
        private double experience = 0.25;
        private double education = 0.10;
        private double project = 0.10;
        private double certification = 0.05;
    }
}

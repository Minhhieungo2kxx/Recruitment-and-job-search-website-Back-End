package com.webjob.application.dto.Response;

import com.webjob.application.models.Entity.Job;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class JobRecommendationScore {
    private Job job;

    private int subSkillScore;
    private int alertScore;

    private int score;

    private Set<String> matchedSkills = new HashSet<>();
    private Set<String> reasons = new HashSet<>();

    public JobRecommendationScore(Job job) {
        this.job = job;
    }


    public int getScore() {
        return (int) Math.round(
                subSkillScore * 0.8 +
                        alertScore * 0.2
        );
    }

    public void addReason(String reason) {
        this.reasons.add(reason);
    }

}

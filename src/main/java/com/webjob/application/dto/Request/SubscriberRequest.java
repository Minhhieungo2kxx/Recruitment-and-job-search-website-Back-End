package com.webjob.application.dto.Request;

import com.webjob.application.models.Entity.Skill;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberRequest {
    private Long id;
    private List<Skill> skills;
}

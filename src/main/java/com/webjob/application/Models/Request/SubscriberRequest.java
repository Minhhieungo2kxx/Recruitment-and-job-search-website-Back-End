package com.webjob.application.Models.Request;

import com.webjob.application.Models.Skill;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

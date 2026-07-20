package com.webjob.application.mapper;
import com.webjob.application.dto.Response.SkillResponse;
import com.webjob.application.dto.Response.SubscriberListResponse;
import com.webjob.application.dto.Response.SubscriberResponse;
import com.webjob.application.models.Entity.Subscriber;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SubscriberMapper {
    public SubscriberResponse mapToResponse(Subscriber subscriber) {
        if (subscriber == null) {
            return null;
        }

        List<SkillResponse> skills = subscriber.getSubscriberSkills() != null
                ? subscriber.getSubscriberSkills().stream()
                .map(ss -> SkillResponse.builder()
                        .id(ss.getSkill().getId())
                        .name(ss.getSkill().getName())
                        .description(ss.getSkill().getDescription())
                        .status(ss.getSkill().getStatus())
                        .build())
                .toList()
                : List.of();

        return SubscriberResponse.builder()
                .id(subscriber.getId())
                .name(subscriber.getName())
                .email(subscriber.getUser().getEmail())
                .phoneNumber(subscriber.getPhoneNumber())
                .description(subscriber.getDescription())
                .status(subscriber.getStatus())
                .subscribed(subscriber.isSubscribed())
                .skills(skills)
                .createdAt(subscriber.getCreatedAt())
                .updatedAt(subscriber.getUpdatedAt())
                .createdBy(subscriber.getCreatedBy())
                .updatedBy(subscriber.getUpdatedBy())
                .build();
    }
    public SubscriberListResponse toResponseSubscriberList(Subscriber subscriber) {
        if (subscriber == null) {
            return null;
        }

        return SubscriberListResponse.builder()
                .id(subscriber.getId())
                .name(subscriber.getName())
                .email(subscriber.getUser().getEmail())
                .status(subscriber.getStatus())
                .build();
    }
}

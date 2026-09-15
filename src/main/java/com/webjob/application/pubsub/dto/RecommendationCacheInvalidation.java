package com.webjob.application.pubsub.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationCacheInvalidation {
   private Long userId;
}

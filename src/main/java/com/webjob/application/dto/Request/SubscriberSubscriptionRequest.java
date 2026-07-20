package com.webjob.application.dto.Request;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberSubscriptionRequest {
    @NotNull
    private Boolean subscribed;
}

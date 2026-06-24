package com.webjob.application.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresenceEvent {
    private UserPresenceDTO presence;
}
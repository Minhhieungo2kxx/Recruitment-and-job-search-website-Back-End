package com.webjob.application.Dto.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresenceEvent {
    private UserPresenceDTO presence;
}
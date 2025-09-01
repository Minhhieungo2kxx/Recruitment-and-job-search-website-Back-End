package com.webjob.application.Models.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PresenceEvent {
    private UserPresenceDTO presence;
}
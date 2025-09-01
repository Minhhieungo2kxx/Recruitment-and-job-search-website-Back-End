package com.webjob.application.Controller;

import com.webjob.application.Models.Response.UserPresenceDTO;
import com.webjob.application.Services.Socket.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
public class PresenceController {
    private final PresenceService presenceService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserPresence(@PathVariable Long userId) {
        UserPresenceDTO presence = presenceService.getUserPresence(userId);
        return ResponseEntity.ok(presence);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getMultipleUsersPresence(@RequestParam List<Long> userIds) {
        List<UserPresenceDTO> presences = userIds.stream()
                .map(presenceService::getUserPresence)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return ResponseEntity.ok(presences);
    }
}

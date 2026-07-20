package com.webjob.application.controller;

import com.webjob.application.annotation.RateLimit;
import com.webjob.application.dto.Response.UserPresenceDTO;
import com.webjob.application.service.Socket.PresenceService;
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


    @RateLimit(maxRequests = 30, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserPresenceDTO> getUserPresence(@PathVariable Long userId) {
        UserPresenceDTO presence = presenceService.getUserPresence(userId);
        return ResponseEntity.ok(presence);
    }

    @RateLimit(maxRequests = 15, timeWindowSeconds = 60, keyType = "TOKEN")
    @GetMapping("/users")
    public ResponseEntity<List<UserPresenceDTO>> getMultipleUsersPresence(@RequestParam List<Long> userIds) {
        return ResponseEntity.ok(presenceService.getAllUserPresence(userIds));
    }
}

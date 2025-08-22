package com.webjob.application.Services.Socket;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    // userId -> set(sessionId)
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    // sessionId -> userId
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    // lastSeen để hiển thị
    private final Map<Long, Instant> lastSeen = new ConcurrentHashMap<>();

    private final Map<Long, Boolean> onlineUsers = new ConcurrentHashMap<>();

    /** Trả về true nếu user chuyển từ Offline -> Online */
    public synchronized boolean addSession(Long userId, String sessionId) {
        sessionToUser.put(sessionId, userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        lastSeen.put(userId, Instant.now());
        return userSessions.get(userId).size() == 1; // từ 0 -> 1 session
    }

    /** Trả về userId nếu user vừa Offline hoàn toàn */
    public synchronized Long removeSession(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId == null) return null;

        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                lastSeen.put(userId, Instant.now());
                return userId; // user này off hẳn
            }
        }
        return null; // vẫn còn session khác => vẫn online
    }

    /** Check online realtime */
    public boolean isUserOnline(Long userId) {
        return userSessions.containsKey(userId);
    }

    public Instant getLastSeen(Long userId) {
        return lastSeen.get(userId);
    }

    public Map<Long, Set<String>> getAllOnlineUsers() {
        return userSessions;
    }
    public void setUserOnline(Long userId) { onlineUsers.put(userId, true); }
    public void setUserOffline(Long userId) { onlineUsers.remove(userId); }
}

package com.webjob.application.Service.Socket;

import com.webjob.application.Model.Entity.User;
import com.webjob.application.Dto.Response.UserPresenceDTO;
import com.webjob.application.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final UserRepository userRepository;
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> userSessionsMap = new ConcurrentHashMap<>();

    // Thêm session mới
    public boolean addSession(Long userId, String sessionId) {
        sessionUserMap.put(sessionId, userId);
        userSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Cập nhật trạng thái online và lastSeenAt
        updateUserOnlineStatus(userId, true);

        // Trả về true nếu user vừa chuyển từ offline sang online
        return userSessionsMap.get(userId).size() == 1;
    }

    // Xóa session
    public Long removeSession(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessionsMap.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessionsMap.remove(userId);
                    // Cập nhật trạng thái offline và lastSeenAt
                    updateUserOnlineStatus(userId, false);
                    return userId;
                }
            }
        }
        return null;
    }

    // Cập nhật trạng thái online/offline trong database
    @Transactional
    public void updateUserOnlineStatus(Long userId, boolean isOnline) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(isOnline);
            user.setLastSeenAt(Instant.now());
            userRepository.save(user);
        });
    }

    // Lấy trạng thái người dùng với text tiếng Việt
    public UserPresenceDTO getUserPresence(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return null;
        }

        UserPresenceDTO dto = new UserPresenceDTO();
        dto.setUserId(userId);
        dto.setOnline(user.isOnline());
        dto.setLastSeenAt(user.getLastSeenAt());

        if (user.isOnline()) {
            dto.setStatusText("Đang hoạt động");
            dto.setStatusType("online");
        } else if (user.getLastSeenAt() != null) {
            String statusText = calculateLastSeenText(user.getLastSeenAt());
            dto.setStatusText(statusText);
            dto.setStatusType(calculateStatusType(user.getLastSeenAt()));
        } else {
            dto.setStatusText("Chưa từng truy cập");
            dto.setStatusType("offline");
        }

        return dto;
    }

    // Tính toán text hiển thị thời gian
    private String calculateLastSeenText(Instant lastSeen) {
        if (lastSeen == null) return "Chưa từng truy cập";

        Duration duration = Duration.between(lastSeen, Instant.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "Vừa truy cập";
        } else if (seconds < 300) { // < 5 phút
            return "Hoạt động " + (seconds / 60) + " phút trước";
        } else if (seconds < 3600) { // < 1 giờ
            long minutes = seconds / 60;
            return "Hoạt động " + minutes + " phút trước";
        } else if (seconds < 86400) { // < 1 ngày
            long hours = seconds / 3600;
            return "Hoạt động " + hours + " giờ trước";
        } else if (seconds < 604800) { // < 1 tuần
            long days = seconds / 86400;
            return "Hoạt động " + days + " ngày trước";
        } else if (seconds < 2592000) { // < 30 ngày
            long weeks = seconds / 604800;
            return "Hoạt động " + weeks + " tuần trước";
        } else if (seconds < 31536000) { // < 1 năm
            long months = seconds / 2592000;
            return "Hoạt động " + months + " tháng trước";
        } else {
            long years = seconds / 31536000;
            return "Hoạt động " + years + " năm trước";
        }
    }

    // Xác định loại trạng thái
    private String calculateStatusType(Instant lastSeen) {
        if (lastSeen == null) return "offline";

        Duration duration = Duration.between(lastSeen, Instant.now());
        long minutes = duration.toMinutes();

        if (minutes < 5) {
            return "recently"; // Vừa mới offline
        } else if (minutes < 30) {
            return "away"; // Vắng mặt tạm thời
        } else {
            return "offline"; // Offline lâu
        }
    }

    // Cập nhật hoạt động định kỳ (gọi khi user có action)
    @Transactional
    public void updateActivity(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(Instant.now());
            userRepository.save(user);
        });
    }
}

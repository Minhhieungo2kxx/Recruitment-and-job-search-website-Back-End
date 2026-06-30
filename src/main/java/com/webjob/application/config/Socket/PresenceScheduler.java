package com.webjob.application.config.Socket;

import com.webjob.application.models.Entity.User;
import com.webjob.application.dto.Response.UserPresenceDTO;
import com.webjob.application.repository.UserRepository;
import com.webjob.application.service.Socket.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceScheduler {
    private final PresenceService presenceService;
    private final PresenceNotifier notifier;
    private final UserRepository userRepository;

    //     Chạy mỗi phút để cập nhật trạng thái "x phút trước"
    @Scheduled(fixedDelay = 120000) // 2 phút
    public void updatePresenceStatus() {
        log.info(" Bắt đầu task updatePresenceStatus()...");
        List<User> recentlyOfflineUsers = userRepository.findRecentlyOfflineUsers(
                Instant.now().minus(Duration.ofMinutes(30))
        );
        for (User user : recentlyOfflineUsers) {
            try {

                if (!user.isOnline()) {
                    UserPresenceDTO presence = presenceService.get_UserPresence(user);
                    notifier.notifyUserPresence(presence);
                }

            } catch (Exception e) {
                log.error(
                        "Presence update failed for user {}",
                        user.getId(),
                        e
                );
            }
        }


        log.info(" Kết thúc task updatePresenceStatus()");
    }

}

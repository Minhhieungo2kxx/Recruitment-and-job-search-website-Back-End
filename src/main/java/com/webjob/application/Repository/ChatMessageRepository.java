package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.ChatMessage;
import com.webjob.application.Model.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    // Lấy lịch sử theo user (cho user đã đăng nhập)
    List<ChatMessage> findByUserOrderByCreatedAtAsc(User user);
    void deleteByUser(User user);
}

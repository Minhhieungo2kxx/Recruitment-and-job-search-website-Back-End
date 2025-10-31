package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.Message;
import com.webjob.application.Model.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
            "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) AND " +
            "m.isDeleted = false " +
            "ORDER BY m.createdAt ASC")
    List<Message> findMessagesBetweenUsers(@Param("userId1") Long userId1,
                                           @Param("userId2") Long userId2);

    @Query("SELECT COUNT(m) FROM Message m WHERE " +
            "m.receiver.id = :userId AND m.sender.id = :senderId AND " +
            "m.status != 'READ' AND m.isDeleted = false")
    Long countUnreadMessagesBetweenUsers(@Param("userId") Long userId,
                                         @Param("senderId") Long senderId);

    @Query("SELECT DISTINCT u FROM User u WHERE " +
            "(u.id IN (SELECT m.sender.id FROM Message m WHERE m.receiver.id = :userId) OR " +
            "u.id IN (SELECT m.receiver.id FROM Message m WHERE m.sender.id = :userId)) AND " +
            "u.id != :userId")
    List<User> findUsersWithConversation(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE " +
            "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
            "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) AND " +
            "m.isDeleted = false " +
            "ORDER BY m.createdAt DESC " +
            "LIMIT 1")
    Optional<Message> findLastMessageBetweenUsers(@Param("userId1") Long userId1,
                                                  @Param("userId2") Long userId2);

    @Modifying
    @Query("UPDATE Message m SET m.status = 'READ' WHERE " +
            "m.receiver.id = :userId AND m.sender.id = :senderId AND m.status != 'read'")
    void markMessagesAsRead(@Param("userId") Long userId, @Param("senderId") Long senderId);

    List<Message> findAllBySenderOrReceiver(User sender, User receiver);

    Page<Message> findAllByStatus(Message.MessageStatus status, Pageable pageable);

    Page<Message> findAllByType(Message.MessageType type, Pageable pageable);

    Page<Message> findAllByIsDeleted(Boolean isDeleted, Pageable pageable);

}

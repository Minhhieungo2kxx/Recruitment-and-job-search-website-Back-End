package com.webjob.application.Repository;

import com.webjob.application.Models.Entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.user1.id = :userId1 AND c.user2.id = :userId2) OR " +
            "(c.user1.id = :userId2 AND c.user2.id = :userId1)")
    Optional<Conversation> findConversationBetweenUsers(@Param("userId1") Long userId1,
                                                        @Param("userId2") Long userId2);

    @Query("SELECT c FROM Conversation c WHERE " +
            "c.user1.id = :userId OR c.user2.id = :userId " +
            "ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") Long userId);
}

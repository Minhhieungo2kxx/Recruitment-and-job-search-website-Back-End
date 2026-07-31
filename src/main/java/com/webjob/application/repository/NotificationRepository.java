package com.webjob.application.repository;

import com.webjob.application.models.Entity.Notification;
import com.webjob.application.models.Entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    long countByUserIdAndReadFalse(Long userId);

    Page<Notification> findByUserId(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    List<Notification> findByUserIdAndReadFalse(Long userId);

    void deleteByUserIdAndReadTrue(Long userId);


}

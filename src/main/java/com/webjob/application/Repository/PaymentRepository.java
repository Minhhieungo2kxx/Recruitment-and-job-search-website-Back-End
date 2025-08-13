package com.webjob.application.Repository;

import com.webjob.application.Models.Entity.Payment;
import com.webjob.application.Models.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

   Optional<Payment> findByUser(User user);


    List<Payment> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.job.id = :jobId AND p.status = 'SUCCESS'")
    Optional<Payment> findSuccessfulPaymentByUserAndJob(@Param("userId") Long userId, @Param("jobId") Long jobId);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.job.id = :jobId ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsByUserAndJob(@Param("userId") Long userId, @Param("jobId") Long jobId);

    boolean existsByUserIdAndJobIdAndStatus(Long userId, Long jobId, String status);
}

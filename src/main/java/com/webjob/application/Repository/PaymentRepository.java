package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.Payment;
import com.webjob.application.Model.Entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment>findByTransactionId(String transactionRef);

    List<Payment> findByUserIdAndStatus(Long userId, String status);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.job.id = :jobId AND p.status = 'SUCCESS'")
    Optional<Payment> findSuccessfulPaymentByUserAndJob(@Param("userId") Long userId, @Param("jobId") Long jobId);

    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId AND p.job.id = :jobId ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsByUserAndJob(@Param("userId") Long userId, @Param("jobId") Long jobId);

    boolean existsByUserIdAndJobIdAndStatus(Long userId, Long jobId, String status);
    boolean existsByUserIdAndJobIdAndStatusAndExpiredAtAfter(Long userId, Long jobId, String status, LocalDateTime expired);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByOrderCode(String orderCode);

    List<Payment> findByStatusAndExpiredAtBefore(String status,LocalDateTime local);

    @Modifying
    @Query(value = """
    DELETE FROM payment
    WHERE status IN ('FAILED', 'EXPIRED')
      AND created_at < NOW() - INTERVAL 6 MONTH
""", nativeQuery = true)
    int deleteOldPayments();



}

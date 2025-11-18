package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    boolean existsByEmail(String email);

    public Subscriber findByEmail(String email);

    List<Subscriber> findAllByEmail(String email);


    @Query("SELECT s.id FROM Subscriber s")
    Page<Long> findPageIds(Pageable pageable);

    @Query("""
            SELECT DISTINCT s
            FROM Subscriber s
            LEFT JOIN FETCH s.skills
            WHERE s.id IN :ids
            """)
    List<Subscriber> findAllWithSkillsByIds(@Param("ids") List<Long> ids);

//    Dùng JOIN FETCH:
//    SELECT s.*, skill.*
//    FROM subscriber s
//    LEFT JOIN skill ON skill.subscriber_id = s.id
//    WHERE s.id IN (1,2,3);
//    → 1 truy vấn duy nhất


//    Tải Subscriber kèm Skills trong 1 truy vấn
//    Tránh N+1 queries
//    Tối ưu hóa hiệu suất cực tốt khi làm việc với quan hệ One-to-Many hoặc Many-to-Many


}

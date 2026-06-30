package com.webjob.application.repository;

import com.webjob.application.models.Entity.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    Optional<Subscriber> findWithSkillsById(Long subscriberId);


//    Dùng JOIN FETCH:
//    SELECT s.*, skill.*
//    FROM subscriber s
//    LEFT JOIN skill ON skill.subscriber_id = s.id
//    WHERE s.id IN (1,2,3);
//    → 1 truy vấn duy nhất


//    Tải Subscriber kèm Skills trong 1 truy vấn
//    Tránh N+1 queries
//    Tối ưu hóa hiệu suất cực tốt khi làm việc với quan hệ One-to-Many hoặc Many-to-Many

//    JOIN: chỉ dùng để nối bảng hoặc lọc dữ liệu, không đảm bảo entity liên quan được nạp vào đối tượng.
//    JOIN FETCH: vừa nối bảng, vừa yêu cầu Hibernate nạp luôn entity/collection liên quan vào bộ nhớ.
//    FetchType.LAZY: chỉ tải dữ liệu liên quan khi bạn thực sự truy cập nó.
//    JOIN FETCH có thể "ghi đè" hành vi LAZY cho truy vấn đó, giúp lấy tất cả trong một lần và tránh phát sinh thêm các câu SQL.
}

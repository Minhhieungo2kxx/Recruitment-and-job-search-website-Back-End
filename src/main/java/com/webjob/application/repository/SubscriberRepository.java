package com.webjob.application.repository;

import com.webjob.application.models.Entity.Subscriber;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long>, JpaSpecificationExecutor<Subscriber> {


    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Subscriber> findAll(Specification<Subscriber> spec, Pageable pageable);


    @Query("SELECT s.id FROM Subscriber s")
    Page<Long> findPageIds(Pageable pageable);

    @Query("""
            select distinct s
            from Subscriber s
            left join fetch s.user
            left join fetch s.subscriberSkills ss
            left join fetch ss.skill
            where s.id = :id
            and s.subscribed = true
            
            """)
    Optional<Subscriber> findSubscriberDetail(@Param("id") Long id);


//    JOIN: chỉ dùng để nối bảng hoặc lọc dữ liệu, không đảm bảo entity liên quan được nạp vào đối tượng.
//    JOIN FETCH: vừa nối bảng, vừa yêu cầu Hibernate nạp luôn entity/collection liên quan vào bộ nhớ.
//    FetchType.LAZY: chỉ tải dữ liệu liên quan khi bạn thực sự truy cập nó.
//    JOIN FETCH có thể "ghi đè" hành vi LAZY cho truy vấn đó, giúp lấy tất cả trong một lần và tránh phát sinh thêm các câu SQL.
}

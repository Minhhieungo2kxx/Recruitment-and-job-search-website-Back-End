package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber,Long> {
    boolean existsByEmail(String email);
    public Subscriber findByEmail(String email);

    List<Subscriber> findAllByEmail(String email);

}

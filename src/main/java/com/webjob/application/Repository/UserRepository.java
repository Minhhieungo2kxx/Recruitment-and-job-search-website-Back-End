package com.webjob.application.Repository;

import com.webjob.application.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    boolean existsByEmail(String email);
    @SuppressWarnings("NullableProblems")
    boolean existsById(Long id);

}

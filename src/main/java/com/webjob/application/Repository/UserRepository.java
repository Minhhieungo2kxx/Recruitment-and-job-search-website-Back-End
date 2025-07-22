package com.webjob.application.Repository;

import com.webjob.application.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    @SuppressWarnings("NullableProblems")
    boolean existsById(Long id);
    User findByEmail(String email);
    User findByEmailAndRefreshToken(String email,String refreshtoken);



}

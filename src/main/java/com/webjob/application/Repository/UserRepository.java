package com.webjob.application.Repository;

import com.webjob.application.Models.Entity.Company;
import com.webjob.application.Models.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);
    @SuppressWarnings("NullableProblems")
    boolean existsById(Long id);
    User findByEmail(String email);
    User findByEmailAndRefreshToken(String email,String refreshtoken);

    Optional<User> findByCompany (Company company);

    List<User> findAllByCompany(Company company);
}

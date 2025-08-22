package com.webjob.application.Repository;

import com.webjob.application.Models.Entity.Company;
import com.webjob.application.Models.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByEmail(String email);

    @SuppressWarnings("NullableProblems")
    boolean existsById(Long id);

    User findByEmail(String email);

    User findByEmailAndRefreshToken(String email, String refreshtoken);

    Optional<User> findByCompany(Company company);

    List<User> findAllByCompany(Company company);


    List<User> findByFullNameContainingIgnoreCaseAndIdNot(String fullName, Long excludeId);

    @Query("SELECT u FROM User u WHERE u.role.name = 'HR' AND u.fullName LIKE %:searchTerm%")
    List<User> findHRsByName(@Param("searchTerm") String searchTerm);

    //    @Query("SELECT u FROM User u WHERE u.role.name != 'HR' AND u.fullName LIKE %:searchTerm%")
//    List<User> findCandidatesByName(@Param("searchTerm") String searchTerm);
    @Query("SELECT u FROM User u WHERE u.role.name NOT IN ('HR', 'ADMIN') AND u.fullName LIKE %:searchTerm%")
    List<User> findCandidatesByName(@Param("searchTerm") String searchTerm);

}

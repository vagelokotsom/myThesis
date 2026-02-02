package com.thesis.backend.repository;

import com.thesis.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findByRole(String role);

    @Query("""
        select u from User u
        where u.role = :role
          and (
            lower(u.username) like lower(concat('%', :query, '%'))
            or lower(u.email) like lower(concat('%', :query, '%'))
          )
        """)
    Page<User> searchByRole(@Param("role") String role, @Param("query") String query, Pageable pageable);
}

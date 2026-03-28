package com.vibes.iam.repository;

import com.vibes.iam.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByEnabledTrue();

    List<User> findByAccountNonLockedFalse();

    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :date")
    List<User> findInactiveUsersSince(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u WHERE u.passwordChangedAt < :date")
    List<User> findUsersWithExpiredPasswords(@Param("date") LocalDateTime date);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
}
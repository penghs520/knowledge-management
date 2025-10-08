package com.enterprise.km.repository;

import com.enterprise.km.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndTenantTenantId(String username, String tenantId);
    Optional<User> findByEmailAndTenantTenantId(String email, String tenantId);
    boolean existsByUsernameAndTenantTenantId(String username, String tenantId);
}

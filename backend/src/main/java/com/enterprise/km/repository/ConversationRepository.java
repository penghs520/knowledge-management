package com.enterprise.km.repository;

import com.enterprise.km.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.user.username = :username " +
           "AND c.tenant.tenantId = :tenantId AND c.deleted = false " +
           "ORDER BY c.updatedAt DESC")
    Page<Conversation> findByUserAndTenant(
        @Param("username") String username,
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    @Query("SELECT c FROM Conversation c WHERE c.id = :id " +
           "AND c.user.username = :username AND c.tenant.tenantId = :tenantId " +
           "AND c.deleted = false")
    Optional<Conversation> findByIdAndUserAndTenant(
        @Param("id") Long id,
        @Param("username") String username,
        @Param("tenantId") String tenantId
    );

    @Query("SELECT c FROM Conversation c WHERE c.user.username = :username " +
           "AND c.tenant.tenantId = :tenantId AND c.isActive = true " +
           "AND c.deleted = false ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveConversations(
        @Param("username") String username,
        @Param("tenantId") String tenantId
    );
}

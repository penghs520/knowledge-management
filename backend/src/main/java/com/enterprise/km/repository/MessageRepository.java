package com.enterprise.km.repository;

import com.enterprise.km.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.deleted = false ORDER BY m.createdAt ASC")
    List<Message> findByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
           "AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Message> findByConversationIdOrderByCreatedAtDesc(
        @Param("conversationId") Long conversationId,
        org.springframework.data.domain.Pageable pageable
    );
}

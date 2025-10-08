package com.enterprise.km.repository;

import com.enterprise.km.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Page<Document> findByTenantTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Page<Document> findByTenantTenantIdAndDepartmentIdAndDeletedFalse(
        String tenantId, Long departmentId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenant.tenantId = :tenantId " +
           "AND d.deleted = false AND (d.title LIKE %:keyword% OR d.content LIKE %:keyword%)")
    Page<Document> searchDocuments(
        @Param("tenantId") String tenantId,
        @Param("keyword") String keyword,
        Pageable pageable);
}

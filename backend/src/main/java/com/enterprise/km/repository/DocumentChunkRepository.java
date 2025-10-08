package com.enterprise.km.repository;

import com.enterprise.km.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(Long documentId);

    List<DocumentChunk> findByDocumentId(Long documentId);

    @Query("SELECT COUNT(*) FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    int countByDocumentId(@Param("documentId") Long documentId);

    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    @Query(value = "SELECT * FROM document_chunks dc " +
           "WHERE dc.document_id IN " +
           "(SELECT d.id FROM documents d WHERE d.tenant_id = :tenantId) " +
           "ORDER BY dc.embedding <-> CAST(:queryEmbedding AS vector) " +
           "LIMIT :limit", nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
        @Param("tenantId") Long tenantId,
        @Param("queryEmbedding") String queryEmbedding,
        @Param("limit") int limit);
}

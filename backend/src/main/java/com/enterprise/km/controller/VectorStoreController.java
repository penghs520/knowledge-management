package com.enterprise.km.controller;

import com.enterprise.km.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vector-store")
@RequiredArgsConstructor
public class VectorStoreController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取向量存储统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<Map<String, Object>> getVectorStoreStats() {
        String sql = "SELECT COUNT(*) as total_vectors FROM vector_store";
        Long totalVectors = jdbcTemplate.queryForObject(sql, Long.class);

        Map<String, Object> stats = Map.of(
            "totalVectors", totalVectors != null ? totalVectors : 0,
            "tableName", "vector_store"
        );

        return ApiResponse.success(stats);
    }

    /**
     * 根据 vectorId 查询向量是否存在
     */
    @GetMapping("/check/{vectorId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<Map<String, Object>> checkVectorExists(@PathVariable String vectorId) {
        String sql = "SELECT COUNT(*) FROM vector_store WHERE id = ?::uuid";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, vectorId);

        Map<String, Object> result = Map.of(
            "vectorId", vectorId,
            "exists", count != null && count > 0
        );

        return ApiResponse.success(result);
    }

    /**
     * 列出所有向量 ID（分页）
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<List<Map<String, Object>>> listVectorIds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String sql = "SELECT id::text as vector_id, " +
                     "LEFT(content, 50) as content_preview, " +
                     "metadata " +
                     "FROM vector_store " +
                     "ORDER BY id " +
                     "LIMIT ? OFFSET ?";

        List<Map<String, Object>> vectors = jdbcTemplate.queryForList(sql, size, page * size);

        return ApiResponse.success(vectors);
    }

    /**
     * 清理孤立的向量数据（在 vector_store 中存在但在 document_chunks 中不存在的）
     */
    @DeleteMapping("/cleanup-orphans")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public ApiResponse<Map<String, Object>> cleanupOrphanVectors() {
        String sql = """
            DELETE FROM vector_store
            WHERE id::text NOT IN (
                SELECT vector_id FROM document_chunks WHERE vector_id IS NOT NULL
            )
            """;

        int deletedCount = jdbcTemplate.update(sql);

        Map<String, Object> result = Map.of(
            "deletedCount", deletedCount,
            "message", "Cleaned up " + deletedCount + " orphan vectors"
        );

        return ApiResponse.success(result);
    }
}

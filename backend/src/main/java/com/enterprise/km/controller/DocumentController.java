package com.enterprise.km.controller;

import com.enterprise.km.dto.ApiResponse;
import com.enterprise.km.model.Document;
import com.enterprise.km.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ApiResponse<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "departmentId", required = false) Long departmentId) {

        Document document = documentService.uploadDocument(file, departmentId);
        return ApiResponse.success("文档上传成功", document);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ApiResponse<Page<Document>> listDocuments(Pageable pageable) {
        Page<Document> documents = documentService.listDocuments(pageable);
        return ApiResponse.success(documents);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ApiResponse<Page<Document>> searchDocuments(
            @RequestParam("q") String keyword,
            Pageable pageable) {

        Page<Document> documents = documentService.searchDocuments(keyword, pageable);
        return ApiResponse.success(documents);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_DELETE')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return ApiResponse.success("删除成功", null);
    }
}

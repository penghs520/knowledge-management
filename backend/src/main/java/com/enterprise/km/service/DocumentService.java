package com.enterprise.km.service;

import com.enterprise.km.model.Document;
import com.enterprise.km.model.DocumentChunk;
import com.enterprise.km.repository.DocumentChunkRepository;
import com.enterprise.km.repository.DocumentRepository;
import com.enterprise.km.repository.TenantRepository;
import com.enterprise.km.repository.UserRepository;
import com.enterprise.km.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final DocumentParserService parserService;
    private final VectorStore vectorStore;
    private final String uploadDir = "uploads/";

    @Transactional
    public Document uploadDocument(MultipartFile file, Long departmentId) {
        try {
            String tenantId = TenantContext.getTenantId();
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // Parse document content
            String content = parserService.parseDocument(file);

            // Save file to disk
            String filePath = saveFile(file, tenantId);

            // Create document entity
            Document document = Document.builder()
                    .title(file.getOriginalFilename())
                    .content(content)
                    .fileName(file.getOriginalFilename())
                    .fileType(parserService.detectMimeType(file))
                    .fileSize(file.getSize())
                    .filePath(filePath)
                    .tenant(tenantRepository.findByTenantId(tenantId).orElseThrow())
                    .uploadedBy(userRepository.findByUsernameAndTenantTenantId(username, tenantId).orElseThrow())
                    .status(Document.DocumentStatus.PROCESSING)
                    .build();

            if (departmentId != null) {
                // Set department if provided
            }

            document = documentRepository.save(document);

            // Process and create chunks
            processDocumentChunks(document, content);

            document.setStatus(Document.DocumentStatus.COMPLETED);
            return documentRepository.save(document);

        } catch (Exception e) {
            log.error("Error uploading document", e);
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    private void processDocumentChunks(Document document, String content) {
        // Split document into chunks
        TokenTextSplitter splitter = new TokenTextSplitter(500, 100, 5, 1000, true);

        List<org.springframework.ai.document.Document> aiDocuments = List.of(
            new org.springframework.ai.document.Document(content)
        );

        List<org.springframework.ai.document.Document> chunks = splitter.apply(aiDocuments);

        // Save chunks to database and vector store
        for (int i = 0; i < chunks.size(); i++) {
            org.springframework.ai.document.Document chunk = chunks.get(i);

            DocumentChunk documentChunk = DocumentChunk.builder()
                    .document(document)
                    .content(chunk.getContent())
                    .chunkIndex(i)
                    .chunkSize(chunk.getContent().length())
                    .vectorId(UUID.randomUUID().toString())
                    .build();

            chunkRepository.save(documentChunk);
        }

        // Add to vector store
        vectorStore.add(chunks);
    }

    private String saveFile(MultipartFile file, String tenantId) throws IOException {
        Path tenantDir = Paths.get(uploadDir, tenantId);
        Files.createDirectories(tenantDir);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = tenantDir.resolve(filename);

        Files.write(filePath, file.getBytes());
        return filePath.toString();
    }

    public Page<Document> listDocuments(Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return documentRepository.findByTenantTenantIdAndDeletedFalse(tenantId, pageable);
    }

    public Page<Document> searchDocuments(String keyword, Pageable pageable) {
        String tenantId = TenantContext.getTenantId();
        return documentRepository.searchDocuments(tenantId, keyword, pageable);
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        String tenantId = TenantContext.getTenantId();
        if (!document.getTenant().getTenantId().equals(tenantId)) {
            throw new RuntimeException("Access denied");
        }

        // 1. Get chunks size before deletion
        int chunkSize = chunkRepository.countByDocumentId(documentId);

        // 1. Delete all chunks from database
        chunkRepository.deleteByDocumentId(documentId);

        // 4. Delete physical file  TODO 后面存储在minio中
        if (document.getFilePath() != null) {
            try {
                Path filePath = Paths.get(document.getFilePath());
                boolean fileDeleted = Files.deleteIfExists(filePath);
                log.info("Physical file deleted: {}", fileDeleted);
            } catch (IOException e) {
                log.warn("Failed to delete physical file {}: {}", document.getFilePath(), e.getMessage());
            }
        }

        // 5. Mark document as deleted (soft delete)
        document.setDeleted(true);
        documentRepository.save(document);

        log.info("Document {} deleted successfully , Chunks: {}, Physical file deleted",
            documentId, chunkSize);
    }
}

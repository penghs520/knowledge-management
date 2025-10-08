package com.enterprise.km.controller;

import com.enterprise.km.dto.ApiResponse;
import com.enterprise.km.dto.QueryRequest;
import com.enterprise.km.dto.QueryResponse;
import com.enterprise.km.service.RAGService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final RAGService ragService;

    @PostMapping("/query")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<QueryResponse> query(@Valid @RequestBody QueryRequest request) {

        String answer = ragService.query(request.getQuestion(), request.getTopK());

        List<Document> sources = ragService.semanticSearchWithScore(
            request.getQuestion(),
            request.getTopK(),
            request.getThreshold()
        );

        List<QueryResponse.SourceDocument> sourceDocs = sources.stream()
                .map(doc -> QueryResponse.SourceDocument.builder()
                        .content(doc.getContent())
                        .documentId(doc.getId())
                        .build())
                .collect(Collectors.toList());

        QueryResponse response = QueryResponse.builder()
                .answer(answer)
                .sources(sourceDocs)
                .build();

        return ApiResponse.success("查询成功", response);
    }

    @PostMapping("/search")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<List<QueryResponse.SourceDocument>> semanticSearch(
            @Valid @RequestBody QueryRequest request) {

        List<Document> documents = ragService.semanticSearchWithScore(
            request.getQuestion(),
            request.getTopK(),
            request.getThreshold()
        );

        List<QueryResponse.SourceDocument> results = documents.stream()
                .map(doc -> QueryResponse.SourceDocument.builder()
                        .content(doc.getContent())
                        .documentId(doc.getId())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.success("搜索成功", results);
    }
}

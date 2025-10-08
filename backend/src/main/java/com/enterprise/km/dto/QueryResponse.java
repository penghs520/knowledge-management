package com.enterprise.km.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    private String answer;
    private List<SourceDocument> sources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceDocument {
        private String content;
        private String documentId;
        private String title;
        private Double score;
    }
}

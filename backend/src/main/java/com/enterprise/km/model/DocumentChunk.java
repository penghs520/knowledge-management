package com.enterprise.km.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false)
    private Integer chunkSize;

    @Column
    private String vectorId; // Reference to vector store

    @Column(columnDefinition = "vector(768)")
    private float[] embedding;

    @Column(columnDefinition = "jsonb")
    private String metadata; // Additional metadata as JSON
}

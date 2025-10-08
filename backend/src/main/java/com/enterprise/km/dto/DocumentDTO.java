package com.enterprise.km.dto;

import com.enterprise.km.model.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {

    private Long id;
    private String title;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String status;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentDTO from(Document document) {
        return DocumentDTO.builder()
                .id(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .status(document.getStatus().toString())
                .tags(document.getTags())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}

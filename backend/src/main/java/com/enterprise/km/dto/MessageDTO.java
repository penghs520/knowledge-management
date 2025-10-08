package com.enterprise.km.dto;

import com.enterprise.km.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private Long id;
    private String role;
    private String content;
    private LocalDateTime createdAt;
    private Integer tokenCount;

    public static MessageDTO from(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .tokenCount(message.getTokenCount())
                .build();
    }
}

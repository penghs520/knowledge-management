package com.enterprise.km.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private Long conversationId; // null for new conversation

    @NotBlank(message = "Question cannot be empty")
    private String question;

    private Integer topK = 5;

    private Double threshold = 0.5;
}

package com.enterprise.km.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class QueryRequest {

    @NotBlank(message = "Question is required")
    private String question;

    @Positive
    private Integer topK = 5;

    private Double threshold = 0.7;
}

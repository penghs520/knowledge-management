package com.enterprise.km.service;

import com.enterprise.km.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RAGService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    private static final String SYSTEM_PROMPT = """
            你是一个企业知识管理系统的AI助手。
            请根据以下上下文信息来回答用户的问题。
            如果上下文中没有相关信息，请明确说明你不知道。
            请用中文回答。

            上下文信息：
            {context}
            """;

    public String query(String question, int topK) {
        String tenantId = TenantContext.getTenantId();
        log.info("Processing RAG query for tenant: {}, question: {}", tenantId, question);

        // Search for relevant documents with lower threshold
        List<Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.query(question)
                .withTopK(topK)
                .withSimilarityThreshold(0.5)  // Lower threshold from 0.7 to 0.5
        );

        log.info("Found {} similar documents", similarDocuments.size());

        if (similarDocuments.isEmpty()) {
            return "抱歉，我在知识库中没有找到与您问题相关的信息。";
        }

        // Build context from similar documents
        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        // Create prompt
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(SYSTEM_PROMPT);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        UserMessage userMessage = new UserMessage(question);

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        // Get response from LLM
        ChatClient chatClient = chatClientBuilder.build();
        String response = chatClient.prompt(prompt)
                .call()
                .content();

        log.info("RAG response generated successfully");
        return response;
    }

    public List<Document> semanticSearch(String query, int topK) {
        log.info("Performing semantic search: {}", query);

        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(0.6)
        );
    }

    public List<Document> semanticSearchWithScore(String query, int topK, double threshold) {
        log.info("Performing semantic search with score: {}", query);

        return vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold)
        );
    }
}

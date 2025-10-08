package com.enterprise.km.service;

import com.enterprise.km.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    private static final String SYSTEM_PROMPT_WITH_HISTORY = """
            你是一个企业知识管理系统的AI助手。
            请根据以下上下文信息和之前的对话历史来回答用户的问题。
            如果上下文中没有相关信息，请明确说明你不知道。
            请用中文回答，并保持对话的连贯性。

            上下文信息：
            {context}
            """;

    /**
     * Query without conversation history (legacy method)
     */
    public String query(String question, int topK) {
        return queryWithHistory(question, topK, null);
    }

    /**
     * Query with conversation history
     */
    public String queryWithHistory(String question, int topK, List<com.enterprise.km.model.Message> conversationHistory) {
        String tenantId = TenantContext.getTenantId();
        log.info("Processing RAG query for tenant: {}, question: {}", tenantId, question);

        // Search for relevant documents
        List<Document> similarDocuments = vectorStore.similaritySearch(
            SearchRequest.query(question)
                .withTopK(topK)
                .withSimilarityThreshold(0.5)
        );

        log.info("Found {} similar documents", similarDocuments.size());

        if (similarDocuments.isEmpty()) {
            return "抱歉，我在知识库中没有找到与您问题相关的信息。";
        }

        // Build context from similar documents
        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        // Build messages list
        List<Message> messages = new ArrayList<>();

        // Add system prompt
        String promptTemplate = (conversationHistory != null && !conversationHistory.isEmpty())
                ? SYSTEM_PROMPT_WITH_HISTORY
                : SYSTEM_PROMPT;
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(promptTemplate);
        Message systemMessage = systemPromptTemplate.createMessage(Map.of("context", context));
        messages.add(systemMessage);

        // Add conversation history (last N messages for context window)
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            int historyLimit = Math.min(conversationHistory.size(), 10); // Limit to last 10 messages
            for (int i = Math.max(0, conversationHistory.size() - historyLimit); i < conversationHistory.size(); i++) {
                com.enterprise.km.model.Message msg = conversationHistory.get(i);
                if (msg.getRole() == com.enterprise.km.model.Message.MessageRole.USER) {
                    messages.add(new UserMessage(msg.getContent()));
                } else if (msg.getRole() == com.enterprise.km.model.Message.MessageRole.ASSISTANT) {
                    messages.add(new AssistantMessage(msg.getContent()));
                }
            }
        }

        // Add current question
        messages.add(new UserMessage(question));

        // Create prompt
        Prompt prompt = new Prompt(messages);

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

package com.enterprise.km.controller;

import com.enterprise.km.dto.*;
import com.enterprise.km.model.Conversation;
import com.enterprise.km.model.Message;
import com.enterprise.km.service.ConversationService;
import com.enterprise.km.service.RAGService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final RAGService ragService;

    /**
     * Create a new conversation
     */
    @PostMapping
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<ConversationDTO> createConversation(@RequestBody(required = false) CreateConversationRequest request) {
        String conversationTitle = (request != null && request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : "新对话";
        Conversation conversation = conversationService.createConversation(conversationTitle);
        return ApiResponse.success("对话创建成功", ConversationDTO.from(conversation));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateConversationRequest {
        private String title;
    }

    /**
     * Get all conversations for current user
     */
    @GetMapping
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<Page<ConversationDTO>> getConversations(Pageable pageable) {
        Page<Conversation> conversations = conversationService.getUserConversations(pageable);
        Page<ConversationDTO> dtos = conversations.map(ConversationDTO::from);
        return ApiResponse.success(dtos);
    }

    /**
     * Get a specific conversation with all messages
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<ConversationDTO> getConversation(@PathVariable Long id) {
        Conversation conversation = conversationService.getConversation(id);
        List<Message> messages = conversationService.getConversationMessages(id);
        conversation.setMessages(messages);
        return ApiResponse.success(ConversationDTO.fromWithMessages(conversation));
    }

    /**
     * Send a message in a conversation (chat)
     */
    @PostMapping("/chat")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        Conversation conversation;
        List<Message> conversationHistory;

        // Create new conversation or use existing one
        if (request.getConversationId() == null) {
            // Extract first few words from question as title
            String title = request.getQuestion().length() > 30
                    ? request.getQuestion().substring(0, 30) + "..."
                    : request.getQuestion();
            conversation = conversationService.createConversation(title);
            conversationHistory = List.of();
        } else {
            conversation = conversationService.getConversation(request.getConversationId());
            conversationHistory = conversationService.getConversationMessages(request.getConversationId());
        }

        // Save user message
        Message userMessage = conversationService.addMessage(
                conversation.getId(),
                Message.MessageRole.USER,
                request.getQuestion()
        );

        // Get AI response with conversation history
        String answer = ragService.queryWithHistory(
                request.getQuestion(),
                request.getTopK(),
                conversationHistory
        );

        // Save assistant message
        Message assistantMessage = conversationService.addMessage(
                conversation.getId(),
                Message.MessageRole.ASSISTANT,
                answer
        );

        ChatResponse response = ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(assistantMessage.getId())
                .answer(answer)
                .documentsFound(request.getTopK())
                .build();

        return ApiResponse.success("查询成功", response);
    }

    /**
     * Delete a conversation
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<Void> deleteConversation(@PathVariable Long id) {
        conversationService.deleteConversation(id);
        return ApiResponse.success("对话已删除", null);
    }

    /**
     * Update conversation title
     */
    @PutMapping("/{id}/title")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<ConversationDTO> updateTitle(
            @PathVariable Long id,
            @RequestBody UpdateTitleRequest request) {
        Conversation conversation = conversationService.updateConversationTitle(id, request.getTitle());
        return ApiResponse.success("标题已更新", ConversationDTO.from(conversation));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTitleRequest {
        private String title;
    }

    /**
     * Get active conversations
     */
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('KNOWLEDGE_QUERY')")
    public ApiResponse<List<ConversationDTO>> getActiveConversations() {
        List<Conversation> conversations = conversationService.getActiveConversations();
        List<ConversationDTO> dtos = conversations.stream()
                .map(ConversationDTO::from)
                .toList();
        return ApiResponse.success(dtos);
    }
}

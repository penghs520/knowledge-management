package com.enterprise.km.service;

import com.enterprise.km.model.Conversation;
import com.enterprise.km.model.Message;
import com.enterprise.km.model.Tenant;
import com.enterprise.km.model.User;
import com.enterprise.km.repository.ConversationRepository;
import com.enterprise.km.repository.MessageRepository;
import com.enterprise.km.repository.TenantRepository;
import com.enterprise.km.repository.UserRepository;
import com.enterprise.km.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @Transactional
    public Conversation createConversation(String title) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getTenantId();

        User user = userRepository.findByUsernameAndTenantTenantId(username, tenantId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Tenant tenant = tenantRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        Conversation conversation = Conversation.builder()
                .title(title)
                .user(user)
                .tenant(tenant)
                .isActive(true)
                .build();

        return conversationRepository.save(conversation);
    }

    public Page<Conversation> getUserConversations(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getTenantId();

        return conversationRepository.findByUserAndTenant(username, tenantId, pageable);
    }

    public Conversation getConversation(Long conversationId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getTenantId();

        return conversationRepository.findByIdAndUserAndTenant(conversationId, username, tenantId)
                .orElseThrow(() -> new RuntimeException("Conversation not found or access denied"));
    }

    public List<Message> getConversationMessages(Long conversationId) {
        // Verify access
        getConversation(conversationId);
        return messageRepository.findByConversationId(conversationId);
    }

    @Transactional
    public Message addMessage(Long conversationId, Message.MessageRole role, String content) {
        Conversation conversation = getConversation(conversationId);

        Message message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();

        return messageRepository.save(message);
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        Conversation conversation = getConversation(conversationId);
        conversation.setDeleted(true);
        conversationRepository.save(conversation);
    }

    @Transactional
    public Conversation updateConversationTitle(Long conversationId, String title) {
        Conversation conversation = getConversation(conversationId);
        conversation.setTitle(title);
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getActiveConversations() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getTenantId();
        return conversationRepository.findActiveConversations(username, tenantId);
    }
}

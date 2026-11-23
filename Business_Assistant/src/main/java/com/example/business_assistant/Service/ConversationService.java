package com.example.business_assistant.service;

import com.example.business_assistant.dto.ConversationVO;
import com.example.business_assistant.dto.MessageVO;
import java.util.List;

public interface ConversationService {
    ConversationVO create(String title, Long userId);
    List<ConversationVO> list(Long userId);
    void delete(Long id, Long userId);
    List<MessageVO> messages(Long conversationId, Long userId);
}

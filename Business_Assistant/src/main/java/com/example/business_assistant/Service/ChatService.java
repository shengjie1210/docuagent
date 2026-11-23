package com.example.business_assistant.Service;

import com.example.business_assistant.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话 Service
 */
public interface ChatService {

    /**
     * 流式问答：检索文档片段 -> 拼 prompt -> 调大模型 -> SSE 推送
     */
    void chatStream(ChatRequest req, Long userId, SseEmitter emitter);
}

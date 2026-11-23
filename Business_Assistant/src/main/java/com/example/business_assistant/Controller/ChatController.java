package com.example.business_assistant.Controller;

import com.example.business_assistant.common.Result;
import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.dto.ChatRequest;
import com.example.business_assistant.Service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话 Controller
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "AI 对话接口")
public class ChatController {

    private final ChatService chatService;
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 流式问答（SSE）
     * POST /api/chat/stream
     * body: { "message": "你的问题" }
     * 返回：text/event-stream，前端逐字接收
     */
    @Operation(summary = "流式问答")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody @Valid ChatRequest req) {
        Long userId = UserContext.getUserId();
        SseEmitter emitter = new SseEmitter(120_000L); // 120 秒超时
        chatService.chatStream(req, userId, emitter);
        return emitter;
    }
}

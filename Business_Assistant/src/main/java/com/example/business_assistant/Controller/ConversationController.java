package com.example.business_assistant.controller;

import com.example.business_assistant.common.Result;
import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.dto.ConversationVO;
import com.example.business_assistant.dto.MessageVO;
import com.example.business_assistant.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversation")
@Tag(name = "对话管理接口")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "新建对话")
    @PostMapping("/create")
    public Result<ConversationVO> create(@RequestBody(required = false) Map<String, String> body) {
        String title = body == null ? null : body.get("title");
        Long userId = UserContext.getUserId();
        return Result.success(conversationService.create(title, userId));
    }

    @Operation(summary = "对话列表")
    @GetMapping("/list")
    public Result<List<ConversationVO>> list() {
        return Result.success(conversationService.list(UserContext.getUserId()));
    }

    @Operation(summary = "删除对话")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        conversationService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "对话历史消息")
    @GetMapping("/{id}/messages")
    public Result<List<MessageVO>> messages(@PathVariable Long id) {
        return Result.success(conversationService.messages(id, UserContext.getUserId()));
    }
}

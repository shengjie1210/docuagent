package com.example.business_assistant.Service.Impl;

import com.example.business_assistant.bean.Conversation;
import com.example.business_assistant.bean.Message;
import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.dto.ChatRequest;
import com.example.business_assistant.dto.DocumentFragment;
import com.example.business_assistant.mapper.ConversationMapper;
import com.example.business_assistant.mapper.MessageMapper;
import com.example.business_assistant.Service.ChatService;
import com.example.business_assistant.service.RagService;
import com.example.business_assistant.tool.DocumentTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final DocumentTools documentTools;

    public ChatServiceImpl(ChatClient chatClient, RagService ragService,
                           ConversationMapper conversationMapper, MessageMapper messageMapper,
                           DocumentTools documentTools) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.documentTools = documentTools;
    }

    @Override
    public void chatStream(ChatRequest req, Long userId, SseEmitter emitter) {
        try {
            Conversation conv = conversationMapper.selectByIdAndUserId(req.getConversationId(), userId);
            if (conv == null) {
                throw BusinessException.notFound("对话不存在");
            }

            Message userMsg = new Message();
            userMsg.setConversationId(req.getConversationId());
            userMsg.setRole("user");
            userMsg.setContent(req.getMessage());
            messageMapper.insert(userMsg);

            if (conv.getTitle() == null || conv.getTitle().isBlank() || "新对话".equals(conv.getTitle())) {
                try {
                    String title = chatClient.prompt()
                            .user("请根据用户的这句话，生成一个不超过12个字的对话标题，只输出标题本身，不要任何解释和标点：" + req.getMessage())
                            .call()
                            .content();
                    if (title != null && !title.isBlank()) {
                        String shortTitle = title.length() > 12 ? title.substring(0, 12) : title;
                        conversationMapper.updateTitle(req.getConversationId(), shortTitle.replaceAll("[\\s\\n]+", ""));
                    }
                } catch (Exception ex) {
                    log.warn("生成对话标题失败: {}", ex.getMessage());
                }
            }

            List<DocumentFragment> fragments = ragService.retrieve(req.getMessage(), userId);

            List<Message> history = messageMapper.selectRecentByConversationId(req.getConversationId(), 10);
            java.util.Collections.reverse(history);

            String systemPrompt = buildSystemPrompt(fragments);

            StringBuilder fullAnswer = new StringBuilder();
            ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                    .system(systemPrompt)
                    .tools(documentTools);

            for (Message m : history) {
                if ("user".equals(m.getRole())) {
                    spec = spec.user(m.getContent());
                } else if ("assistant".equals(m.getRole())) {
                    spec = spec.messages(new AssistantMessage(m.getContent()));
                }
            }

            spec.stream().chatResponse()
                    .doOnNext(response -> {
                        var result = response.getResult();
                        if (result != null && result.getOutput() != null) {
                            String token = result.getOutput().getText();
                            if (token != null) {
                                fullAnswer.append(token);
                                try {
                                    // 流式 token 不清洗，让前端用 Markdown 渲染（** 变粗体，- 变列表）
                                    emitter.send(SseEmitter.event().name("message").data(token));
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    })
                    .doOnComplete(() -> {
                        String finalAnswer = cleanOutput(fullAnswer.toString());

                        Message aiMsg = new Message();
                        aiMsg.setConversationId(req.getConversationId());
                        aiMsg.setRole("assistant");
                        aiMsg.setContent(finalAnswer);
                        if (!fragments.isEmpty()) {
                            aiMsg.setSources(fragments.toString());
                        }
                        messageMapper.insert(aiMsg);

                        try {
                            emitter.send(SseEmitter.event().name("done")
                                    .data(java.util.Map.of(
                                            "answer", finalAnswer,
                                            "sources", fragments
                                    )));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnError(emitter::completeWithError)
                    .subscribe();

        } catch (BusinessException e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        } catch (Exception e) {
            log.error("对话失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("对话失败，请稍后重试"));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    private String buildSystemPrompt(List<DocumentFragment> fragments) {
        if (fragments == null || fragments.isEmpty()) {
            return "你是 DocuAgent 的专属文档助手。"
                    + "用户问具体业务问题时，先在【参考资料】里找；"
                    + "找不到就直接回复：「抱歉，我在已上传的文档中没有找到关于这个问题的信息。」"
                    + "禁止用你训练时见过的企业报销 / 客服 / HR 通用常识补全答案。";
        }
        List<DocumentFragment> sorted = fragments.stream()
                .sorted((a, b) -> Double.compare(
                        b.getScore() == null ? 0 : b.getScore(),
                        a.getScore() == null ? 0 : a.getScore()))
                .toList();
        String context = sorted.stream()
                .map(f -> "【来源文档：" + f.getFilename() + "｜相关度=" + String.format("%.2f", f.getScore() == null ? 0 : f.getScore()) + "】\n" + f.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
        return "你是 DocuAgent 的专属文档助手，严格遵守以下规则：\n"
                + "1. 只能使用下方【参考资料】中明确出现的事实作答，包括数字、电话、日期、条款编号、金额、姓名。\n"
                + "2. 参考资料没提到的内容，绝对不要用你自己的企业报销 / 客服 / HR / IT 常识补全。例如：\n"
                + "   - 问「能报什么费用」时，只能列参考资料里明确写的费用类型（如交通、住宿），没写的「餐补 / 业务招待费 / 培训费 / 团建费」一律说「文档中未提及」。\n"
                + "   - 问「客服热线」时，只能列参考资料里出现的号码，不要自己补充「400 热线 / 邮箱 / 工作时间」等没写的细节。\n"
                + "3. 输出用自然中文段落，绝对不要保留 Markdown 语法符号：粗体 **、斜体 *、列表 -、表格 |、标题 #、代码 `。\n"
                + "4. 答案引用了具体条款时，在句中自然带出条款编号即可，不要写「根据 XXX.md 第 X 条」这种生硬引用。\n"
                + "5. 若多份参考资料互相矛盾，选最新发布日期那一份，并在答案开头说明采用的依据。\n"
                + "6. 如果检索到的片段只是和问题「字面上相关」但语义不匹配，宁可说找不到，也不要硬答。\n\n"
                + "【参考资料】\n" + context;
    }

    /**
     * 输出清洗：把模型返回的 Markdown 符号从最终答案里剥掉。
     * 实战项目改进：模型有时不听 prompt 指令，这里兜底做一次正则清洗。
     */
    private String cleanOutput(String raw) {
        if (raw == null) return "";
        String s = raw;
        // 粗体 **xxx** 或 __xxx__ -> xxx
        s = s.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        s = s.replaceAll("__(.+?)__", "$1");
        // 行首列表符号 "- " / "* " / "• " -> 去掉
        s = s.replaceAll("(?m)^\\s*[-*•]\\s+", "");
        // 有序列表 "1. " -> 去掉
        s = s.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");
        // 表格竖线 | -> 空格
        s = s.replaceAll("\\|", " ");
        // 标题 # -> 去掉
        s = s.replaceAll("(?m)^#{1,6}\\s*", "");
        // 代码块 `xxx` -> xxx
        s = s.replaceAll("`(.+?)`", "$1");
        // 多个连续空行压缩成一个
        s = s.replaceAll("\\n{3,}", "\\n\\n");
        return s.trim();
    }
}
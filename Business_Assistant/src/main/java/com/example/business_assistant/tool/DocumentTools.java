package com.example.business_assistant.tool;

import com.example.business_assistant.bean.Document;
import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.mapper.DocumentMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档工具集（Function Call）
 * <p>
 * 大模型可以自动调用这些方法来：
 * - 搜索用户的文档列表
 * - 查看最近上传的文档
 * - 获取文档详情
 */
@Component
public class DocumentTools {

    private final DocumentMapper documentMapper;

    public DocumentTools(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    @Tool(description = "根据关键词搜索当前用户的文档列表，返回文档ID和文件名")
    public List<DocumentInfo> searchDocuments(
            @ToolParam(description = "搜索关键词，匹配文件名") String keyword) {
        Long userId = UserContext.getUserId();
        // 简单实现：查当前用户所有文档，按关键词过滤
        List<Document> docs = documentMapper.selectPage(userId, keyword, 0, 20);
        return docs.stream()
                .map(d -> new DocumentInfo(d.getId(), d.getFilename(), d.getFileType()))
                .collect(Collectors.toList());
    }

    @Tool(description = "列出当前用户最近上传的文档")
    public List<DocumentInfo> listRecentDocuments(
            @ToolParam(description = "返回数量，默认5", required = false) Integer limit) {
        Long userId = UserContext.getUserId();
        int size = limit == null ? 5 : Math.min(limit, 20);
        List<Document> docs = documentMapper.selectPage(userId, null, 0, size);
        return docs.stream()
                .map(d -> new DocumentInfo(d.getId(), d.getFilename(), d.getFileType()))
                .collect(Collectors.toList());
    }

    /**
     * 文档信息（给大模型看的简化版）
     */
    public record DocumentInfo(Long id, String filename, String fileType) {}
}

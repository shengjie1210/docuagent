package com.example.business_assistant.service;

import com.example.business_assistant.bean.Document;
import com.example.business_assistant.dto.DocumentFragment;

import java.util.List;

/**
 * RAG Service 接口
 * <p>
 * 负责：
 *  1. 文档上传后：读取文本 -> 切分 chunk -> 向量化 -> 存 VectorStore
 *  2. 提问时：问题向量化 -> 检索 top-k 相关片段
 */
public interface RagService {

    /**
     * 文档入库：读取文本 -> 切分 -> 向量化 -> 存向量库
     * @param doc 文档实体（包含文件路径）
     * @return 实际切分出的块数
     */
    int indexDocument(Document doc);

    /**
     * 检索与问题最相关的文档片段
     * @param question 用户问题
     * @param userId   当前用户（只搜自己的文档）
     * @return 相关片段列表
     */
    List<DocumentFragment> retrieve(String question, Long userId);

    /**
     * 删除某篇文档在向量库中的所有片段（删文档时调用，避免残留片段被检索到）
     * @param docId 文档 ID
     */
    void deleteByDocId(Long docId);
}

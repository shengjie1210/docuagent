package com.example.business_assistant.service;

import com.example.business_assistant.common.PageResult;
import com.example.business_assistant.dto.BatchUploadResult;
import com.example.business_assistant.dto.DocumentVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档 Service 接口
 */
public interface DocumentService {

    /**
     * 上传文档：校验类型/大小 -> 存文件 -> 写数据库
     */
    DocumentVO upload(MultipartFile file, Long userId);

    /**
     * 批量上传文档：逐个调用单文件 upload，单个失败不中断整批。
     * @param files 文件数组
     * @param userId 当前用户 ID
     * @return 每个文件的处理结果 + 汇总统计
     */
    BatchUploadResult uploadBatch(MultipartFile[] files, Long userId);

    /**
     * 分页查询文档列表
     */
    PageResult<DocumentVO> list(String keyword, int page, int size, Long userId);

    /**
     * 文档详情
     */
    DocumentVO getById(Long id, Long userId);

    /**
     * 读取文档原文（纯文本）
     */
    String getContent(Long id, Long userId);

    /**
     * 删除文档（同时删物理文件）
     */
    void delete(Long id, Long userId);

    /**
     * 重新索引文档（历史文档补切分 / 解析失败重试）
     */
    DocumentVO reindex(Long id, Long userId);
}

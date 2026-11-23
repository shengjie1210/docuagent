package com.example.business_assistant.mapper;

import com.example.business_assistant.bean.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文档 Mapper 接口
 * SQL 写在 resources/mapper/DocumentMapper.xml
 */
@Mapper
public interface DocumentMapper {

    /**
     * 插入文档记录，主键回填到 document.id
     */
    int insert(Document document);

    /**
     * 根据 ID 查文档（同时校验属于当前用户）
     */
    Document selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 分页查询当前用户的文档列表
     * @param keyword  文件名关键词（可空）
     * @param offset   偏移量 (page-1)*size
     * @param size     每页条数
     */
    List<Document> selectPage(@Param("userId") Long userId,
                              @Param("keyword") String keyword,
                              @Param("offset") int offset,
                              @Param("size") int size);

    /**
     * 查询当前用户文档总数（配合分页）
     */
    long count(@Param("userId") Long userId, @Param("keyword") String keyword);

    /**
     * 根据 ID 删除文档（校验用户归属）
     */
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 索引完成后回写切分块数、状态，以及 AI 生成的摘要（可空）
     */
    int updateIndexResult(@Param("id") Long id,
                          @Param("chunkCount") Integer chunkCount,
                          @Param("status") String status,
                          @Param("summary") String summary);

    /** 全库文档总数（管理员统计用） */
    long countAll();

    /** 全库 chunk 总数（document.chunk_count 求和） */
    long sumChunkCount();
}

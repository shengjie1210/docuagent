package com.example.business_assistant.mapper;

import com.example.business_assistant.bean.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ConversationMapper {
    int insert(Conversation conversation);
    List<Conversation> selectByUserId(@Param("userId") Long userId);
    Conversation selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    int updateTitle(@Param("id") Long id, @Param("title") String title);
    /** 全库对话总数（管理员统计用） */
    long countAll();
}

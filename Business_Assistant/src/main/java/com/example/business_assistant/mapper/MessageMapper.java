package com.example.business_assistant.mapper;

import com.example.business_assistant.bean.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MessageMapper {
    int insert(Message message);
    List<Message> selectByConversationId(@Param("conversationId") Long conversationId);
    List<Message> selectRecentByConversationId(@Param("conversationId") Long conversationId, @Param("limit") int limit);
    int deleteByConversationId(@Param("conversationId") Long conversationId);
    /** 全库消息总数（管理员统计用） */
    long countAll();
}

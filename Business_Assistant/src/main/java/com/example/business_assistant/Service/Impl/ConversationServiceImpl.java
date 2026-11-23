package com.example.business_assistant.service.impl;

import com.example.business_assistant.bean.Conversation;
import com.example.business_assistant.bean.Message;
import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.dto.ConversationVO;
import com.example.business_assistant.dto.MessageVO;
import com.example.business_assistant.mapper.ConversationMapper;
import com.example.business_assistant.mapper.MessageMapper;
import com.example.business_assistant.service.ConversationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public ConversationVO create(String title, Long userId) {
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle(title == null || title.isBlank() ? "新对话" : title);
        conversationMapper.insert(conv);
        ConversationVO vo = new ConversationVO();
        BeanUtils.copyProperties(conv, vo);
        return vo;
    }

    @Override
    public List<ConversationVO> list(Long userId) {
        return conversationMapper.selectByUserId(userId).stream().map(c -> {
            ConversationVO vo = new ConversationVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id, Long userId) {
        Conversation conv = conversationMapper.selectByIdAndUserId(id, userId);
        if (conv == null) throw BusinessException.notFound("对话不存在");
        messageMapper.deleteByConversationId(id);
        conversationMapper.deleteByIdAndUserId(id, userId);
    }

    @Override
    public List<MessageVO> messages(Long conversationId, Long userId) {
        Conversation conv = conversationMapper.selectByIdAndUserId(conversationId, userId);
        if (conv == null) throw BusinessException.notFound("对话不存在");
        return messageMapper.selectByConversationId(conversationId).stream().map(m -> {
            MessageVO vo = new MessageVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}

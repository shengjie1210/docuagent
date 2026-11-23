package com.example.business_assistant.bean;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Message {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private String sources;
    private LocalDateTime createdAt;
}

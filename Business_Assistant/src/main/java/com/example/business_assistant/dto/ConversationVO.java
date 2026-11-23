package com.example.business_assistant.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
}

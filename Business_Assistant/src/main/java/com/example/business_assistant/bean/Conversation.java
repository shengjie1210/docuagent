package com.example.business_assistant.bean;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Conversation {
    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createdAt;
}

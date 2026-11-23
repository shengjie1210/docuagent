package com.example.business_assistant.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private String role;
    private String content;
    private String sources;
    private LocalDateTime createdAt;
}

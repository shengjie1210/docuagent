package com.example.business_assistant.service;

import com.example.business_assistant.bean.AiProviderConfig;
import com.example.business_assistant.dto.ProviderSaveRequest;
import java.util.List;

public interface ProviderService {
    List<AiProviderConfig> list(Long userId);
    Long save(ProviderSaveRequest req, Long userId);
    void delete(Long id, Long userId);
    void setDefault(Long id, Long userId);
    boolean testConnection(ProviderSaveRequest req);
}

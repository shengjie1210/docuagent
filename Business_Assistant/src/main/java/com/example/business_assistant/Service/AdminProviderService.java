package com.example.business_assistant.service;

import com.example.business_assistant.dto.ProviderSaveRequest;
import com.example.business_assistant.dto.ProviderTestResult;
import com.example.business_assistant.dto.ProviderVO;

import java.util.List;
import java.util.Map;

/** 管理员视角的供应商配置服务（跨所有用户） */
public interface AdminProviderService {
    List<ProviderVO> list();
    Long save(ProviderSaveRequest req);
    void update(Long id, ProviderSaveRequest req);
    void delete(Long id);
    void enable(Long id);
    ProviderTestResult test(Long id);
    ProviderTestResult testDraft(ProviderSaveRequest req);
    Map<String, Object> balance(Long id);
}

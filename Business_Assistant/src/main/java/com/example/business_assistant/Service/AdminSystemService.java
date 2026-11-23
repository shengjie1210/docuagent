package com.example.business_assistant.service;

import com.example.business_assistant.dto.SystemInfoVO;

import java.util.List;

/** 管理员 - 系统监控服务 */
public interface AdminSystemService {
    SystemInfoVO getSystemInfo();
    /** 读日志最后 N 行，倒序返回（最新在前） */
    List<String> readLogs(String type, int lines);
}

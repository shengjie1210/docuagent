package com.example.business_assistant.dto;

import lombok.Data;

/** 系统监控信息 */
@Data
public class SystemInfoVO {
    // JVM 内存（MB）
    private long heapUsedMb;
    private long heapMaxMb;
    private double heapUsedPercent;
    private long nonHeapUsedMb;
    private long nonHeapMaxMb;

    // CPU / 线程
    private double cpuUsagePercent;
    private int threadCount;

    // 运行时长
    private long startTime;
    private String startTimeText;
    private long uptimeMs;
    private String uptimeText;

    // DB 连接池（HikariCP）
    private int dbActiveConnections;
    private int dbIdleConnections;
    private int dbTotalConnections;
    private int dbMaxConnections;

    // 业务数据量
    /** 全站文档总数 */
    private long documentCount;
    /** 当前登录用户自己的文档数（与"文档管理"页一致） */
    private long myDocumentCount;
    private long conversationCount;
    private long messageCount;

    // 健康检查
    private String serverTime;
    private String appVersion;
    private String javaVersion;
    private String osName;
}

package com.example.business_assistant.controller;

import com.example.business_assistant.common.Result;
import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.dto.ProviderSaveRequest;
import com.example.business_assistant.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/provider")
@Tag(name = "AI 服务商配置")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @Operation(summary = "服务商配置列表")
    @GetMapping("/list")
    public Result list() {
        return Result.success(providerService.list(UserContext.getUserId()));
    }

    @Operation(summary = "新增/更新配置")
    @PostMapping("/save")
    public Result<Long> save(@RequestBody @Valid ProviderSaveRequest req) {
        return Result.success(providerService.save(req, UserContext.getUserId()));
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        providerService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "设为默认")
    @PostMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        providerService.setDefault(id, UserContext.getUserId());
        return Result.success();
    }

    @Operation(summary = "测试连通性")
    @PostMapping("/test")
    public Result<Map<String, Object>> test(@RequestBody ProviderSaveRequest req) {
        boolean ok = providerService.testConnection(req);
        return Result.success(Map.of("success", ok));
    }
}

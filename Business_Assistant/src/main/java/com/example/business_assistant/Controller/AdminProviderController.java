package com.example.business_assistant.controller;

import com.example.business_assistant.common.Result;
import com.example.business_assistant.dto.ProviderSaveRequest;
import com.example.business_assistant.dto.ProviderTestResult;
import com.example.business_assistant.dto.ProviderVO;
import com.example.business_assistant.service.AdminProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 管理员 - 供应商管理。所有接口走 AdminAuthInterceptor 校验管理员权限。 */
@RestController
@RequestMapping("/api/admin/providers")
@Tag(name = "管理员-供应商管理")
public class AdminProviderController {

    private final AdminProviderService adminProviderService;

    public AdminProviderController(AdminProviderService adminProviderService) {
        this.adminProviderService = adminProviderService;
    }

    @Operation(summary = "列出所有供应商配置（apiKey 脱敏）")
    @GetMapping
    public Result<List<ProviderVO>> list() {
        return Result.success(adminProviderService.list());
    }

    @Operation(summary = "新增供应商配置")
    @PostMapping
    public Result<Long> save(@RequestBody @Valid ProviderSaveRequest req) {
        return Result.success(adminProviderService.save(req));
    }

    @Operation(summary = "更新供应商配置（apiKey 留空=不修改）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ProviderSaveRequest req) {
        adminProviderService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除供应商配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminProviderService.delete(id);
        return Result.success();
    }

    @Operation(summary = "切换为默认启用")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        adminProviderService.enable(id);
        return Result.success();
    }

    @Operation(summary = "测试已有配置的连通性")
    @PostMapping("/{id}/test")
    public Result<ProviderTestResult> test(@PathVariable Long id) {
        return Result.success(adminProviderService.test(id));
    }

    @Operation(summary = "测试草稿配置的连通性（未保存）")
    @PostMapping("/test")
    public Result<ProviderTestResult> testDraft(@RequestBody ProviderSaveRequest req) {
        return Result.success(adminProviderService.testDraft(req));
    }

    @Operation(summary = "余额/额度查询")
    @GetMapping("/{id}/balance")
    public Result<Map<String, Object>> balance(@PathVariable Long id) {
        return Result.success(adminProviderService.balance(id));
    }
}

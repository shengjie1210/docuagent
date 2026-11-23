package com.example.business_assistant.Controller;

import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.common.Result;
import com.example.business_assistant.dto.SystemInfoVO;
import com.example.business_assistant.service.AdminSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理员 - 系统监控 */
@RestController
@RequestMapping("/api/admin/system")
@Tag(name = "管理员-系统监控")
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    public AdminSystemController(AdminSystemService adminSystemService) {
        this.adminSystemService = adminSystemService;
    }

    @Operation(summary = "系统信息：JVM/CPU/线程/连接池/数据量")
    @GetMapping("/info")
    public Result<SystemInfoVO> info() {
        return Result.success(adminSystemService.getSystemInfo());
    }

    @Operation(summary = "读 info.log 最后 N 行（倒序，最新在前）")
    @GetMapping("/logs")
    public Result<List<String>> logs(@RequestParam(defaultValue = "100") int lines) {
        if (lines < 1 || lines > 1000) throw BusinessException.badRequest("lines 需在 1-1000 之间");
        return Result.success(adminSystemService.readLogs("info", lines));
    }

    @Operation(summary = "读 error.log 最后 N 行（倒序，最新在前）")
    @GetMapping("/logs/error")
    public Result<List<String>> errorLogs(@RequestParam(defaultValue = "50") int lines) {
        if (lines < 1 || lines > 1000) throw BusinessException.badRequest("lines 需在 1-1000 之间");
        return Result.success(adminSystemService.readLogs("error", lines));
    }
}

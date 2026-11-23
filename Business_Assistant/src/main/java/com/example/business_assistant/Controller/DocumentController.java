package com.example.business_assistant.controller;

import com.example.business_assistant.common.PageResult;
import com.example.business_assistant.common.Result;
import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.dto.BatchUploadResult;
import com.example.business_assistant.dto.DocumentVO;
import com.example.business_assistant.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档管理 Controller
 * <p>
 * 所有接口都需要登录（JWT 拦截器自动校验），
 * 当前用户 ID 从 UserContext 取，不用前端传。
 */
@RestController
@RequestMapping("/api/document")
@Tag(name = "文档接口")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * 上传文档
     * POST /api/document/upload
     * form-data: file=@xxx.pdf
     */
    @Operation(summary = "上传文档")
    @PostMapping("/upload")
    public Result<DocumentVO> upload(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        return Result.success(documentService.upload(file, userId));
    }

    /**
     * 批量上传文档
     * POST /api/document/uploadBatch
     * form-data: files=@a.pdf&files=@b.docx
     */
    @Operation(summary = "批量上传文档（最多 10 个）")
    @PostMapping("/uploadBatch")
    public Result<BatchUploadResult> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        Long userId = UserContext.getUserId();
        return Result.success(documentService.uploadBatch(files, userId));
    }

    /**
     * 文档列表（分页 + 关键词搜索）
     * GET /api/document/list?keyword=退货&page=1&size=10
     */
    @Operation(summary = "文档列表")
    @GetMapping("/list")
    public Result<PageResult<DocumentVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContext.getUserId();
        return Result.success(documentService.list(keyword, page, size, userId));
    }

    /**
     * 文档详情
     * GET /api/document/1
     */
    @Operation(summary = "文档详情")
    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(documentService.getById(id, userId));
    }

    /**
     * 文档原文预览
     * GET /api/document/1/content
     */
    @Operation(summary = "文档原文")
    @GetMapping("/{id}/content")
    public Result<String> getContent(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(documentService.getContent(id, userId));
    }

    /**
     * 删除文档
     * DELETE /api/document/1
     */
    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        documentService.delete(id, userId);
        return Result.success();
    }

    /**
     * 重新索引文档（切分 + 向量化）
     * POST /api/document/1/reindex
     */
    @Operation(summary = "重新索引文档")
    @PostMapping("/{id}/reindex")
    public Result<DocumentVO> reindex(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(documentService.reindex(id, userId));
    }
}

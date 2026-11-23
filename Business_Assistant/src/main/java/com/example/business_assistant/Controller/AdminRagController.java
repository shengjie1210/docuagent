package com.example.business_assistant.Controller;

import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.common.Result;
import com.example.business_assistant.config.RagConfig;
import com.example.business_assistant.dto.RagConfigUpdateRequest;
import com.example.business_assistant.dto.RagConfigVO;
import com.example.business_assistant.mapper.DocumentMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 管理员 - RAG 配置热更新 */
@RestController
@RequestMapping("/api/admin/rag-config")
@Tag(name = "管理员-RAG配置")
public class AdminRagController {

    private final RagConfig ragConfig;
    private final DocumentMapper documentMapper;

    @Value("${docuagent.vector-store-path:./data/vectors.json}")
    private String vectorStorePath;

    public AdminRagController(RagConfig ragConfig, DocumentMapper documentMapper) {
        this.ragConfig = ragConfig;
        this.documentMapper = documentMapper;
    }

    @Operation(summary = "查看当前 RAG 参数与向量库统计")
    @GetMapping
    public Result<RagConfigVO> get() {
        long bytes = 0;
        try {
            Path p = Paths.get(vectorStorePath);
            if (Files.exists(p)) bytes = Files.size(p);
        } catch (Exception e) {
            // 读不到就返回 0，不影响配置读取
        }
        RagConfigVO vo = new RagConfigVO(
                ragConfig.getChunkSize(),
                ragConfig.getTopK(),
                ragConfig.getSimilarityThreshold(),
                ragConfig.getMaxChunksPerDoc(),
                bytes,
                documentMapper.sumChunkCount(),
                documentMapper.countAll()
        );
        return Result.success(vo);
    }

    @Operation(summary = "热更新 RAG 参数（仅内存生效，重启后恢复配置）")
    @PutMapping
    public Result<RagConfigVO> update(@RequestBody RagConfigUpdateRequest req) {
        if (req.getChunkSize() != null) {
            if (req.getChunkSize() < 100 || req.getChunkSize() > 5000)
                throw BusinessException.badRequest("chunkSize 需在 100-5000 之间");
            ragConfig.setChunkSize(req.getChunkSize());
        }
        if (req.getTopK() != null) {
            if (req.getTopK() < 1 || req.getTopK() > 20)
                throw BusinessException.badRequest("topK 需在 1-20 之间");
            ragConfig.setTopK(req.getTopK());
        }
        if (req.getSimilarityThreshold() != null) {
            double t = req.getSimilarityThreshold();
            if (t < 0 || t > 1)
                throw BusinessException.badRequest("similarityThreshold 需在 0-1 之间");
            ragConfig.setSimilarityThreshold(t);
        }
        if (req.getMaxChunksPerDoc() != null) {
            if (req.getMaxChunksPerDoc() < 1 || req.getMaxChunksPerDoc() > 20)
                throw BusinessException.badRequest("maxChunksPerDoc 需在 1-20 之间");
            ragConfig.setMaxChunksPerDoc(req.getMaxChunksPerDoc());
        }
        return get();
    }
}

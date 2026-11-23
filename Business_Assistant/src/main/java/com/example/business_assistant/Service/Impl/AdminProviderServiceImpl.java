package com.example.business_assistant.service.impl;

import com.example.business_assistant.bean.AiProviderConfig;
import com.example.business_assistant.bean.User;
import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.common.UserContext;
import com.example.business_assistant.dto.ProviderSaveRequest;
import com.example.business_assistant.dto.ProviderTestResult;
import com.example.business_assistant.dto.ProviderVO;
import com.example.business_assistant.mapper.AiProviderConfigMapper;
import com.example.business_assistant.mapper.UserMapper;
import com.example.business_assistant.service.AdminProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员供应商配置服务实现
 * <p>
 * - 跨用户列出所有配置，apiKey 一律脱敏
 * - 编辑时 apiKey 留空 = 不修改原值
 * - 连通性测试用真实 HTTP 调 chat/completions，返回延迟和状态
 */
@Slf4j
@Service
public class AdminProviderServiceImpl implements AdminProviderService {

    private final AiProviderConfigMapper providerMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AdminProviderServiceImpl(AiProviderConfigMapper providerMapper, UserMapper userMapper) {
        this.providerMapper = providerMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<ProviderVO> list() {
        List<AiProviderConfig> configs = providerMapper.selectAll();
        return configs.stream().map(this::toVO).toList();
    }

    private ProviderVO toVO(AiProviderConfig c) {
        ProviderVO vo = new ProviderVO();
        BeanUtils.copyProperties(c, vo);
        vo.setApiKey(maskKey(c.getApiKey()));
        User u = userMapper.selectById(c.getUserId());
        vo.setUsername(u == null ? String.valueOf(c.getUserId()) : u.getUsername());
        return vo;
    }

    /** 脱敏：前 6 位 + ****** + 后 4 位（长度不足则全遮） */
    private String maskKey(String key) {
        if (key == null || key.isBlank()) return "";
        if (key.length() <= 10) return "******";
        return key.substring(0, 6) + "******" + key.substring(key.length() - 4);
    }

    @Override
    @Transactional
    public Long save(ProviderSaveRequest req) {
        AiProviderConfig config = new AiProviderConfig();
        BeanUtils.copyProperties(req, config);
        config.setId(null);
        // 归属到当前操作的管理员账号
        config.setUserId(UserContext.getUserId());
        // 第一个配置自动设为默认
        if (providerMapper.selectAll().isEmpty()) {
            config.setIsDefault(1);
        } else {
            config.setIsDefault(0);
        }
        providerMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional
    public void update(Long id, ProviderSaveRequest req) {
        AiProviderConfig existing = providerMapper.selectById(id);
        if (existing == null) throw BusinessException.notFound("供应商配置不存在");
        AiProviderConfig config = new AiProviderConfig();
        BeanUtils.copyProperties(req, config);
        config.setId(id);
        config.setUserId(existing.getUserId());
        // apiKey 留空时不进 SQL（见 updateById 的 <if>），保留原值
        providerMapper.updateById(config);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (providerMapper.selectById(id) == null) throw BusinessException.notFound("供应商配置不存在");
        providerMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void enable(Long id) {
        if (providerMapper.selectById(id) == null) throw BusinessException.notFound("供应商配置不存在");
        providerMapper.clearAllDefault();
        providerMapper.setDefaultById(id);
    }

    @Override
    public ProviderTestResult test(Long id) {
        AiProviderConfig c = providerMapper.selectById(id);
        if (c == null) throw BusinessException.notFound("供应商配置不存在");
        return doTest(c.getBaseUrl(), c.getApiKey(), c.getChatModel());
    }

    @Override
    public ProviderTestResult testDraft(ProviderSaveRequest req) {
        return doTest(req.getBaseUrl(), req.getApiKey(), req.getChatModel());
    }

    /**
     * 真实调一次 OpenAI 兼容的 chat/completions 测连通性。
     * 编辑场景下 apiKey 可能留空（表示用已保存的），这里若为空则取默认配置的 key。
     */
    private ProviderTestResult doTest(String baseUrl, String apiKey, String chatModel) {
        if (apiKey == null || apiKey.isBlank()) {
            AiProviderConfig def = providerMapper.selectDefaultByUserId(UserContext.getUserId());
            if (def != null) apiKey = def.getApiKey();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            return new ProviderTestResult(false, 0, "base_url 不能为空");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new ProviderTestResult(false, 0, "api_key 不能为空");
        }
        String url = buildChatCompletionsUrl(baseUrl);
        String model = (chatModel == null || chatModel.isBlank()) ? "qwen-plus" : chatModel;
        log.info("供应商连通性测试: url={}, model={}", url, model);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        body.put("max_tokens", 5);
        body.put("stream", false);
        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return new ProviderTestResult(false, 0, "请求序列化失败: " + e.getMessage());
        }

        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return new ProviderTestResult(true, latency, "HTTP " + resp.statusCode() + " · 连通正常");
            }
            return new ProviderTestResult(false, latency, "HTTP " + resp.statusCode() + " · " + abbreviate(resp.body()));
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("供应商连通性测试失败: url={}, err={}", url, e.getMessage());
            return new ProviderTestResult(false, latency, "连接失败: " + e.getMessage());
        }
    }

    /**
     * 构造 OpenAI 兼容的 chat/completions 地址。
     * 阿里云百炼兼容端点形如 .../compatible-mode，真实接口是 .../compatible-mode/v1/chat/completions，
     * 少了 /v1 会 404。这里智能补版本路径：
     * <ul>
     *   <li>baseUrl 已以 /v1 结尾（含尾斜杠） → 直接拼 /chat/completions</li>
     *   <li>baseUrl 的任意路径段里已含 /v1     → 直接拼 /chat/completions</li>
     *   <li>否则                                → 补 /v1/chat/completions</li>
     * </ul>
     */
    private String buildChatCompletionsUrl(String baseUrl) {
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // 已包含 /v1 路径段（如 https://x/v1 或 https://x/compatible-mode/v1）
        String lower = base.toLowerCase();
        if (lower.endsWith("/v1") || lower.contains("/v1/")) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }

    /**
     * 余额查询：优先尝试阿里云百炼 quota 接口（若 base_url 为 DashScope），
     * 否则退化为 chat/completions 连通性测试（能调通即视为可用）。
     */
    @Override
    public Map<String, Object> balance(Long id) {
        AiProviderConfig c = providerMapper.selectById(id);
        if (c == null) throw BusinessException.notFound("供应商配置不存在");
        ProviderTestResult test = doTest(c.getBaseUrl(), c.getApiKey(), c.getChatModel());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", test.isSuccess());
        result.put("latencyMs", test.getLatencyMs());
        result.put("message", test.getMessage());
        result.put("note", "余额/额度接口需云厂商 AK/SK 签名，当前以 chat/completions 连通性代替");
        return result;
    }

    private String abbreviate(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }
}

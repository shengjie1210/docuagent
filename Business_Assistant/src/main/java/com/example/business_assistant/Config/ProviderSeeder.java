package com.example.business_assistant.Config;

import com.example.business_assistant.bean.AiProviderConfig;
import com.example.business_assistant.mapper.AiProviderConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 启动时把当前 application.yml 里生效的 AI 配置种子化到数据库。
 *
 * <p>背景：项目早期 AI 配置写死在 yml，ai_provider_config 表从未被写入，
 * 导致管理后台"供应商管理"列表为空、看起来像没配置。本类在应用启动时：
 * <ol>
 *   <li>如果表完全为空 → 把 yml 的 base-url/api-key/model 写入一条默认配置；</li>
 *   <li>如果表已有数据 → 不做任何事（绝不覆盖用户在后台的改动）。</li>
 * </ol>
 *
 * <p>用 @Order(Ordered.HIGHEST_PRECEDENCE) 保证在 ChatModel bean 之前把配置种进去。
 */
@Slf4j
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ProviderSeeder implements ApplicationRunner {

    private final AiProviderConfigMapper mapper;

    @Value("${spring.ai.openai.base-url:}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:qwen-plus}")
    private String chatModel;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-v3}")
    private String embedModel;

    public ProviderSeeder(AiProviderConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!mapper.selectAll().isEmpty()) {
                log.info("供应商配置表已有数据，跳过种子化");
                return;
            }
            if (baseUrl == null || baseUrl.isBlank()) {
                log.warn("application.yml 未配置 spring.ai.openai.base-url，跳过种子化");
                return;
            }
            AiProviderConfig cfg = new AiProviderConfig();
            cfg.setUserId(1L);                       // 归属管理员账号（id=1）
            cfg.setProvider("dashscope");
            cfg.setName("默认配置（来自 application.yml）");
            cfg.setBaseUrl(baseUrl);
            cfg.setApiKey(apiKey);
            cfg.setChatModel(chatModel);
            cfg.setEmbedModel(embedModel);
            cfg.setIsDefault(1);
            mapper.insert(cfg);
            log.info("已从 application.yml 种子化默认供应商配置: baseUrl={}, chatModel={}, embedModel={}",
                    baseUrl, chatModel, embedModel);
        } catch (Exception e) {
            // 种子化失败不应阻断应用启动（例如表结构未就绪），运行时仍可回退到 yml
            log.warn("种子化供应商配置失败（不影响启动，运行时将回退到 yml）: {}", e.getMessage());
        }
    }
}

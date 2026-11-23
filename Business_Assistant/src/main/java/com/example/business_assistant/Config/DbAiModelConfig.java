package com.example.business_assistant.Config;

import com.example.business_assistant.bean.AiProviderConfig;
import com.example.business_assistant.mapper.AiProviderConfigMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

/**
 * 让 AI 运行时真正使用数据库里的供应商配置（管理后台可管理）。
 *
 * <p>Spring AI 默认从 application.yml 装配 OpenAiApi/ChatModel/EmbeddingModel，
 * 写死在配置文件里，后台改了不生效。这里提供 @Primary 的等价 bean，优先从
 * ai_provider_config 表的默认配置构建；表里没数据或字段缺失时回退到 yml，
 * 保证系统永远可用。
 *
 * <p>改动后台配置后需重启后端生效（换 embed_model 还需重新索引文档）。
 */
@Slf4j
@Configuration
public class DbAiModelConfig {

    private final AiProviderConfigMapper mapper;

    @Value("${spring.ai.openai.base-url:}")
    private String ymlBaseUrl;
    @Value("${spring.ai.openai.api-key:}")
    private String ymlApiKey;
    @Value("${spring.ai.openai.chat.options.model:qwen-plus}")
    private String ymlChatModel;
    @Value("${spring.ai.openai.embedding.options.model:text-embedding-v3}")
    private String ymlEmbedModel;

    public DbAiModelConfig(AiProviderConfigMapper mapper) {
        this.mapper = mapper;
    }

    /** 取 default=1 的配置；没有默认就取第一条；查库失败返回 null（回退 yml） */
    private AiProviderConfig effective() {
        try {
            List<AiProviderConfig> all = mapper.selectAll();
            if (all == null || all.isEmpty()) return null;
            return all.stream()
                    .filter(c -> c.getIsDefault() != null && c.getIsDefault() == 1)
                    .findFirst()
                    .orElse(all.get(0));
        } catch (Exception e) {
            log.warn("读取数据库供应商配置失败，运行时回退到 application.yml: {}", e.getMessage());
            return null;
        }
    }

    private static String pick(String dbValue, String fallback) {
        return (dbValue != null && !dbValue.isBlank()) ? dbValue : fallback;
    }

    private static String mask(String key) {
        if (key == null || key.length() <= 10) return "******";
        return key.substring(0, 6) + "******" + key.substring(key.length() - 4);
    }

    @Bean("dbOpenAiApi")
    @Primary
    public OpenAiApi dbOpenAiApi() {
        AiProviderConfig c = effective();
        String baseUrl = pick(c == null ? null : c.getBaseUrl(), ymlBaseUrl);
        String apiKey = pick(c == null ? null : c.getApiKey(), ymlApiKey);
        log.info("AI 运行时供应商配置来源={}, baseUrl={}, apiKey={}",
                c == null ? "application.yml" : "database",
                baseUrl, mask(apiKey));
        return OpenAiApi.builder().baseUrl(baseUrl).apiKey(apiKey).build();
    }

    @Bean("dbChatModel")
    @Primary
    public OpenAiChatModel dbChatModel(
            @Qualifier("dbOpenAiApi") OpenAiApi api,
            ObjectProvider<ToolCallingManager> toolCallingManager,
            ObjectProvider<RetryTemplate> retryTemplate,
            ObjectProvider<ObservationRegistry> observationRegistry) {
        AiProviderConfig c = effective();
        String model = pick(c == null ? null : c.getChatModel(), ymlChatModel);

        var builder = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).build());
        toolCallingManager.ifAvailable(builder::toolCallingManager);
        retryTemplate.ifAvailable(builder::retryTemplate);
        observationRegistry.ifAvailable(builder::observationRegistry);
        log.info("AI 对话模型: {}", model);
        return builder.build();
    }

    @Bean("dbEmbeddingModel")
    @Primary
    public OpenAiEmbeddingModel dbEmbeddingModel(@Qualifier("dbOpenAiApi") OpenAiApi api) {
        AiProviderConfig c = effective();
        String model = pick(c == null ? null : c.getEmbedModel(), ymlEmbedModel);
        log.info("AI 向量模型: {}", model);
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder().model(model).build());
    }
}

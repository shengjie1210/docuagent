package com.example.business_assistant.Config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring AI 配置类
 * <p>
 * 1. ChatClient：全局默认 system prompt
 * 2. SimpleVectorStore：内存向量库，启动时从 vectors.json 加载，关闭时保存
 */
@Slf4j
@Configuration
public class SpringAIConfig {

    /** 向量库持久化文件路径 */
    private static final String VECTOR_STORE_FILE = "./data/vectors.json";

    /** 持有的向量库实例，关闭时用它保存（不要再调 bean 方法，否则销毁阶段会报错） */
    private SimpleVectorStore vectorStore;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一个文档助手，请根据提供的资料回答用户问题。" +
                        "如果资料中没有相关信息，明确告诉用户未在文档中找到。不要编造。"
                )
                .build();
    }

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        Path filePath = Paths.get(VECTOR_STORE_FILE).toAbsolutePath().normalize();
        if (Files.exists(filePath)) {
            try {
                store.load(filePath.toFile());
                log.info("向量库加载成功: {}", filePath);
            } catch (Exception e) {
                log.warn("向量库加载失败: {}", e.getMessage());
            }
        }
        this.vectorStore = store;
        return store;
    }

    /**
     * 应用关闭时自动保存向量库到 vectors.json
     */
    @PreDestroy
    public void saveVectorStore() {
        if (vectorStore == null) {
            return;
        }
        try {
            Path filePath = Paths.get(VECTOR_STORE_FILE).toAbsolutePath().normalize();
            Files.createDirectories(filePath.getParent());
            vectorStore.save(filePath.toFile());
            log.info("向量库已保存到: {}", filePath);
        } catch (Exception e) {
            log.error("向量库保存失败", e);
        }
    }
}
package com.example.business_assistant.service.impl;

import com.example.business_assistant.bean.AiProviderConfig;
import com.example.business_assistant.common.BusinessException;
import com.example.business_assistant.dto.ProviderSaveRequest;
import com.example.business_assistant.mapper.AiProviderConfigMapper;
import com.example.business_assistant.service.ProviderService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProviderServiceImpl implements ProviderService {

    private final AiProviderConfigMapper providerMapper;

    public ProviderServiceImpl(AiProviderConfigMapper providerMapper) {
        this.providerMapper = providerMapper;
    }

    @Override
    public List<AiProviderConfig> list(Long userId) {
        List<AiProviderConfig> list = providerMapper.selectByUserId(userId);
        // 不返回 apiKey 明文
        list.forEach(c -> c.setApiKey(maskKey(c.getApiKey())));
        return list;
    }

    @Override
    public Long save(ProviderSaveRequest req, Long userId) {
        AiProviderConfig config = new AiProviderConfig();
        BeanUtils.copyProperties(req, config);
        config.setUserId(userId);

        if (req.getId() == null) {
            // 新增：如果是第一个配置，自动设为默认
            List<AiProviderConfig> existing = providerMapper.selectByUserId(userId);
            config.setIsDefault(existing.isEmpty() ? 1 : 0);
            providerMapper.insert(config);
        } else {
            AiProviderConfig existing = providerMapper.selectByIdAndUserId(req.getId(), userId);
            if (existing == null) throw BusinessException.notFound("配置不存在");
            config.setId(req.getId());
            providerMapper.update(config);
        }
        return config.getId();
    }

    @Override
    public void delete(Long id, Long userId) {
        AiProviderConfig config = providerMapper.selectByIdAndUserId(id, userId);
        if (config == null) throw BusinessException.notFound("配置不存在");
        providerMapper.deleteByIdAndUserId(id, userId);
    }

    @Override
    public void setDefault(Long id, Long userId) {
        AiProviderConfig config = providerMapper.selectByIdAndUserId(id, userId);
        if (config == null) throw BusinessException.notFound("配置不存在");
        providerMapper.clearDefault(userId);
        config.setIsDefault(1);
        providerMapper.update(config);
    }

    @Override
    public boolean testConnection(ProviderSaveRequest req) {
        // 简单实现：返回 true，不真正调 API
        // 后续可以调一下 /models 接口测试连通性
        return true;
    }

    /**
     * apiKey 脱敏：只显示前 4 位和后 4 位
     */
    private String maskKey(String key) {
        if (key == null || key.length() < 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}

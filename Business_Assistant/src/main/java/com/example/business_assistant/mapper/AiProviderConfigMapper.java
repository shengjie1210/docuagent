package com.example.business_assistant.mapper;

import com.example.business_assistant.bean.AiProviderConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AiProviderConfigMapper {
    int insert(AiProviderConfig config);
    List<AiProviderConfig> selectByUserId(@Param("userId") Long userId);
    AiProviderConfig selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    AiProviderConfig selectDefaultByUserId(@Param("userId") Long userId);
    int update(AiProviderConfig config);
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
    int clearDefault(@Param("userId") Long userId);

    // ---- 管理员视角（跨所有用户）----
    /** 列出所有用户的供应商配置 */
    List<AiProviderConfig> selectAll();
    /** 按主键查（不限用户） */
    AiProviderConfig selectById(@Param("id") Long id);
    /** 按主键更新（管理员用，apiKey 传 null 时不覆盖） */
    int updateById(AiProviderConfig config);
    /** 按主键删除（管理员用） */
    int deleteById(@Param("id") Long id);
    /** 清除所有用户的默认标记 */
    int clearAllDefault();
    /** 按主键设为默认 */
    int setDefaultById(@Param("id") Long id);
}

package com.example.business_assistant.mapper;

import com.example.business_assistant.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper 接口
 * <p>
 * SQL 写在 resources/mapper/UserMapper.xml 里，
 * 这里只定义方法，MyBatis 通过 namespace 找到对应 XML。
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查用户（注册时查重、登录时查账号）
     * @return 找不到返回 null
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据 ID 查用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 插入新用户，主键会自动回填到 user.id
     */
    int insert(User user);
}

package com.example.business_assistant.common;

import lombok.Data;
import java.util.List;

/**
 * 通用分页结果包装类
 * @param <T> 列表元素类型
 */
@Data
public class PageResult<T> {

    /** 总条数 */
    private long total;

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码 */
    private int page;

    /** 每页条数 */
    private int size;

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}

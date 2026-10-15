package com.owner.common.core.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页返回结果。
 *
 * @param <T> 行数据类型
 */
@Data
@NoArgsConstructor
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 数据行 */
    private List<T> rows = Collections.emptyList();

    /** 总记录数 */
    private long total;

    /** 当前页码，从 1 开始 */
    private long pageNum = 1;

    /** 每页条数 */
    private long pageSize = 10;

    /** 总页数 */
    private long pages;

    public PageResult(List<T> rows, long total, long pageNum, long pageSize) {
        this.rows = rows == null ? Collections.emptyList() : rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return new PageResult<>(Collections.emptyList(), 0L, pageNum, pageSize);
    }

    public static <T> PageResult<T> of(List<T> rows, long total, long pageNum, long pageSize) {
        return new PageResult<>(rows, total, pageNum, pageSize);
    }
}

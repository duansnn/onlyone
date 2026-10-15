package com.owner.common.core.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询基础参数。
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /** 默认每页条数 */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /** 单页最大条数，防止一次拉取过多数据 */
    public static final long MAX_PAGE_SIZE = 500L;

    /** 页码，从 1 开始 */
    private Long pageNum = DEFAULT_PAGE_NUM;

    /** 每页条数 */
    private Long pageSize = DEFAULT_PAGE_SIZE;

    public long getPageNumOrDefault() {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    public long getPageSizeOrDefault() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}

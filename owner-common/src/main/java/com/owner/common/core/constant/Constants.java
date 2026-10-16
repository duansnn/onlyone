package com.owner.common.core.constant;

/**
 * 全局常量。
 */
public interface Constants {

    /** UTF-8 编码 */
    String UTF8 = "UTF-8";

    /** 成功标记 */
    String SUCCESS = "0";

    /** 失败标记 */
    String FAIL = "1";

    /** 是 */
    String YES = "Y";

    /** 否 */
    String NO = "N";

    /** 逻辑删除：未删除 */
    Integer NOT_DELETED = 0;

    /** 逻辑删除：已删除 */
    Integer DELETED = 1;

    /** 超级管理员标识 */
    Long SUPER_ADMIN_ID = 1L;

    /** 默认页大小 */
    int DEFAULT_PAGE_SIZE = 10;

    /** 网关/链路追踪请求头 */
    String HEADER_TRACE_ID = "X-Trace-Id";

    /** 认证请求头 */
    String HEADER_AUTHORIZATION = "Authorization";

    /** Token 前缀 */
    String TOKEN_PREFIX = "Bearer ";

    /** Redis Key 前缀 */
    String REDIS_PREFIX = "owner:";
}

package com.owner.common.core.enums;

import lombok.Getter;

/**
 * 统一业务状态码。
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),
    /** 参数校验失败 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未认证 */
    UNAUTHORIZED(401, "未认证或登录已过期"),
    /** 无权限 */
    FORBIDDEN(403, "没有访问权限"),
    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),
    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    /** 服务器内部错误 */
    ERROR(500, "系统异常，请稍后重试"),
    /** 服务暂不可用 */
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    /** 业务失败 */
    BUSINESS_ERROR(600, "业务处理失败"),
    /** 数据已存在 */
    DATA_EXISTS(601, "数据已存在"),
    /** 数据不存在 */
    DATA_NOT_FOUND(602, "数据不存在"),
    /** 重复提交 */
    REPEAT_SUBMIT(603, "请勿重复提交");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

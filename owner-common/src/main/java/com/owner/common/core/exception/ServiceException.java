package com.owner.common.core.exception;

import com.owner.common.core.enums.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 业务异常，由全局异常处理器统一转换为 {@code R.fail(...)}。
 */
@Getter
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务状态码 */
    private final int code;

    public ServiceException(String msg) {
        super(msg);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public ServiceException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public ServiceException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    /** 使用状态码枚举，但自定义提示信息 */
    public ServiceException(ResultCode resultCode, String msg) {
        super(msg);
        this.code = resultCode.getCode();
    }

    public ServiceException(String msg, Throwable cause) {
        super(msg, cause);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public ServiceException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMsg(), cause);
        this.code = resultCode.getCode();
    }

    public static ServiceException of(String msg) {
        return new ServiceException(msg);
    }

    public static ServiceException of(ResultCode resultCode) {
        return new ServiceException(resultCode);
    }
}

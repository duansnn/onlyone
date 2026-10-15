package com.owner.framework.handler;

import com.owner.common.core.domain.R;
import com.owner.common.core.enums.ResultCode;
import com.owner.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：把异常统一转换为 {@link R} 结构。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("[业务异常] {} {} -> code={}, msg={}",
                request.getMethod(), request.getRequestURI(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 上的参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {}", msg);
        return R.fail(ResultCode.BAD_REQUEST, msg.isEmpty() ? ResultCode.BAD_REQUEST.getMsg() : msg);
    }

    /** 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[参数绑定失败] {}", msg);
        return R.fail(ResultCode.BAD_REQUEST, msg.isEmpty() ? ResultCode.BAD_REQUEST.getMsg() : msg);
    }

    /** 方法参数级校验失败（@Validated + @RequestParam 等） */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public R<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        log.warn("[方法参数校验失败] {}", e.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, ResultCode.BAD_REQUEST.getMsg());
    }

    /** 单参数约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("[约束校验失败] {}", msg);
        return R.fail(ResultCode.BAD_REQUEST, msg.isEmpty() ? ResultCode.BAD_REQUEST.getMsg() : msg);
    }

    /** 缺少必填请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[缺少请求参数] {}", e.getParameterName());
        return R.fail(ResultCode.BAD_REQUEST, "缺少必填参数：" + e.getParameterName());
    }

    /** 请求体解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[请求体解析失败] {}", e.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误，无法解析");
    }

    /** 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[请求方法不支持] {}", e.getMessage());
        return R.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /** 静态资源不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("[资源不存在] {}", e.getResourcePath());
        return R.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("[系统异常] {} {}", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ResultCode.ERROR);
    }
}

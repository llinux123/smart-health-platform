package com.smart.health.common.exception;

import com.smart.health.common.result.Result;
import com.smart.health.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败: {}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数: {}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, e.getMessage());
    }

    /**
     * 请求参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {} = {}", e.getName(), e.getValue());
        return Result.fail(ResultCode.PARAM_ERROR,
                "参数「" + e.getName() + "」格式不正确");
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParamException(MissingServletRequestParameterException e) {
        log.warn("缺少必填参数: {}", e.getParameterName());
        return Result.fail(ResultCode.PARAM_ERROR,
                "缺少必填参数「" + e.getParameterName() + "」");
    }

    /**
     * Multipart 上传解析异常
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleMultipartException(MultipartException e, HttpServletRequest req) {
        log.warn("Multipart解析失败: method={} uri={} reason={}",
                req.getMethod(), req.getRequestURI(), extractRootCause(e));
        return Result.fail(ResultCode.PARAM_ERROR, "文件上传格式错误，请重新选择文件后上传");
    }

    /**
     * 文件超出大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Result<?> handleMaxUploadSize() {
        return Result.fail(ResultCode.PARAM_ERROR, "文件大小超过限制");
    }

    /**
     * 系统未知异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail("系统内部错误，请稍后重试");
    }

    private String extractRootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}

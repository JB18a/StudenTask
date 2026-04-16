package com.example.ccc.common;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 * 作用：拦截所有 Controller 抛出的异常，统一返回 JSON 格式
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 拦截：文件上传大小超限异常
     * 场景：用户上传了超过 Spring Boot 默认限制（通常是 1MB）的文件
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<String> handleMaxSizeException(MaxUploadSizeExceededException e) {
        return Result.error(400, "文件大小超出限制，请上传较小的文件！");
    }

    /**
     * 2. 拦截：数据库重复键异常
     * 场景：注册时使用了已存在的用户名（触发了数据库 uk_username 索引）
     * 场景：申请发布者时，user_id 已经存在于 publisher 表
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result<String> handleDuplicateKeyException(DuplicateKeyException e) {
        return Result.error(400, "数据已存在，请勿重复操作（如用户名或发布者身份已存在）");
    }

    /**
     * 3. 拦截：空指针异常 (开发中最常见的 bug)
     */
    @ExceptionHandler(NullPointerException.class)
    public Result<String> handleNullPointerException(NullPointerException e) {
        e.printStackTrace(); // 打印到控制台方便开发排查
        return Result.error(500, "系统数据异常(NPE)，请联系管理员");
    }

    /**
     * 4. 拦截：通用的运行时异常
     * 场景：你在代码里手动 throw new RuntimeException("密码错误")
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        return Result.error(400, e.getMessage()); // 直接把你的报错文字返给前端
    }

    /**
     * 5. 兜底：拦截所有未知的其他异常
     * 作用：防止系统崩溃后返回 HTML 页面
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace(); // 打印错误栈到控制台
        return Result.error(500, "服务器内部错误，请稍后重试");
    }
    /**
     * 捕获 @Validated 校验失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        // 获取第一个错误信息
        FieldError fieldError = e.getBindingResult().getFieldError();
        String errorMsg = fieldError != null ? fieldError.getDefaultMessage() : "参数错误";
        return Result.error(400, errorMsg);
    }
}
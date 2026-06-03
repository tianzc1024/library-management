package com.library.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器，统一返回 {code, message, data} 格式。
 * 替代各个Controller中分散的try-catch。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 500);
        body.put("message", e.getMessage() != null ? e.getMessage() : "服务器内部错误");
        body.put("data", null);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        log.error("未知异常: {}", e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 500);
        body.put("message", "服务器内部错误");
        body.put("data", null);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}

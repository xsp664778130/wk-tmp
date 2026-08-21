package com.skillport.server.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = BrowserController.class)
public class BrowserErrorHandler {
    private static final String INVALID_INPUT = "请检查填写内容。";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> responseStatus(ResponseStatusException exception) {
        String reason = exception.getReason();
        String message = reason == null || reason.isBlank() ? "请求没有完成，请稍后再试。" : reason;
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("error", message));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidInput(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", INVALID_INPUT));
    }
}

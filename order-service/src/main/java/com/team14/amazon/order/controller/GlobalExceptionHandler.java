//package com.team14.amazon.order.controller;
//
//import com.fasterxml.jackson.databind.exc.InvalidFormatException;
//import org.springframework.dao.DataIntegrityViolationException;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.converter.HttpMessageNotReadableException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//@ControllerAdvice
//public class GlobalExceptionHandler {
//
//    // Handle invalid arguments (e.g., invalid date format or invalid status)
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("status", HttpStatus.BAD_REQUEST.value());
//        body.put("error", "Bad Request");
//        body.put("message", ex.getMessage());  // e.g., "Invalid date format. Use YYYY-MM-DD"
//        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
//    }
//
//
//
//    //This exception happens when the incoming request is invalid
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<Map<String, Object>> handleInvalidJson(HttpMessageNotReadableException ex) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("timestamp", LocalDateTime.now());
//        body.put("status", HttpStatus.BAD_REQUEST.value());
//        body.put("error", "Bad Request");
//
//        // Check if the cause is an InvalidFormatException (e.g., wrong date format)
//        if (ex.getCause() instanceof InvalidFormatException invalidFormat) {
//            String targetType = invalidFormat.getTargetType().getSimpleName();
//            String value = invalidFormat.getValue().toString();
//            body.put("message", "Invalid " + targetType + " value: " + value);
//        } else {
//            body.put("message", "Malformed JSON request");
//        }
//
//        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
//    }
//
//
//    //This exception basically catches the error that happens after
//    //The DB constraints are violated
//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("timestamp", LocalDateTime.now());
//        body.put("status", HttpStatus.BAD_REQUEST.value());
//        body.put("error", "Bad Request");
//        body.put("message", "Database constraint violation: required field missing");
//        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
//        Map<String, Object> body = new HashMap<>();
//        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//        body.put("error", "A generic error occurred/maybe from server or maybe not");
//        body.put("message", ex.getMessage());
//        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//}
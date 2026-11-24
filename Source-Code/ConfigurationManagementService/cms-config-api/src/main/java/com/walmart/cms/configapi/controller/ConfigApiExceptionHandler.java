package com.walmart.cms.configapi.controller;
import com.walmart.cms.dal.exception.StaleConfigurationVersionException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice
public class ConfigApiExceptionHandler {
 @ExceptionHandler(StaleConfigurationVersionException.class) public ResponseEntity<Map<String,Object>> stale(StaleConfigurationVersionException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error","STALE_CONFIGURATION_VERSION","message",ex.getMessage())); }
 @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<Map<String,Object>> missing(NoSuchElementException ex) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","NOT_FOUND","message",ex.getMessage())); }
}

package com.walmart.cms.deploymentapi.controller;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice
public class DeploymentApiExceptionHandler {
 @ExceptionHandler(NoSuchElementException.class)
 ResponseEntity<Map<String,String>> notFound(RuntimeException e){return ResponseEntity.status(404).body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(IllegalArgumentException.class)
 ResponseEntity<Map<String,String>> badRequest(RuntimeException e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));}
 @ExceptionHandler(IllegalStateException.class)
 ResponseEntity<Map<String,String>> conflict(RuntimeException e){return ResponseEntity.status(409).body(Map.of("error",e.getMessage()));}
}

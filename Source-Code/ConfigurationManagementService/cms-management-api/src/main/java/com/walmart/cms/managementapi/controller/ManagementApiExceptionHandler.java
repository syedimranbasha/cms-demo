package com.walmart.cms.managementapi.controller;

import com.walmart.cms.managementapi.service.ManagementService.ConflictException;
import com.walmart.cms.managementapi.service.ManagementService.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManagementApiExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail notFound(ResourceNotFoundException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,e.getMessage()); }
    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,e instanceof ConflictException ? e.getMessage() : "A resource with the supplied unique key already exists"); }
    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException e) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Request validation failed"); }
}

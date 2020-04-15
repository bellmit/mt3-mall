package com.hw.clazz;

import com.hw.aggregate.product.exception.CategoryNotFoundException;
import com.hw.aggregate.product.exception.NotEnoughActualStorageException;
import com.hw.aggregate.product.exception.NotEnoughOrderStorageException;
import com.hw.aggregate.product.exception.ProductNotFoundException;
import com.hw.shared.ErrorMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DomainExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = {
            CategoryNotFoundException.class,
            NotEnoughActualStorageException.class,
            NotEnoughOrderStorageException.class,
            ProductNotFoundException.class,
    })
    protected ResponseEntity<?> handle400Exception(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMessage(ex), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}
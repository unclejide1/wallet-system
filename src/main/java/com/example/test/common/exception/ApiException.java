package com.example.test.common.exception;


import com.example.test.common.api.ApiError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<ApiError> errors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of(ApiError.global(message)));
    }

    public ApiException(HttpStatus status, String message, List<ApiError> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }
}

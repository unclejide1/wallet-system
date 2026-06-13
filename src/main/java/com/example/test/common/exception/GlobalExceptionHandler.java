package com.example.test.common.exception;

import com.example.test.common.api.ApiError;
import com.example.test.common.api.ApiResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception, HttpServletRequest request) {
        return buildErrorResponse(exception.getStatus(), exception.getMessage(), exception.getErrors(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toApiError)
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpServletRequest request
    ) {
        List<ApiError> errors = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> ApiError.of(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiError> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> ApiError.of(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter '%s'".formatted(exception.getName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, List.of(ApiError.of(exception.getName(), message)), request);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        Throwable cause = exception.getCause();

        // Check if the error happened because a value couldn't be mapped to its expected type (like an Enum)
        if (cause instanceof InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                // Extract the field name that caused the issue (e.g., "gender")
                String fieldName = invalidFormatException.getPath().stream()
                        .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                        .reduce((first, second) -> second) // Gets the exact leaf field
                        .orElse("field");

                String invalidValue = invalidFormatException.getValue().toString();

                // Extract the list of valid options from the Enum dynamically
                String acceptedValues = java.util.Arrays.stream(targetType.getEnumConstants())
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(", "));

                String detailedMessage = "Invalid value '%s' for field '%s'. Accepted options are: [%s]"
                        .formatted(invalidValue, fieldName, acceptedValues);

                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Validation failed",
                        List.of(ApiError.of(fieldName, detailedMessage)),
                        request
                );
            }
        }

        // Fallback for completely broken JSON structure (e.g., missing curly braces, trailing commas)
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON payload structure or syntax error",
                List.of(ApiError.global("Request body is missing or structurally malformed")),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource",
                List.of(ApiError.global("You do not have permission to access this resource")),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        ConstraintErrorDetails constraintErrorDetails = resolveConstraintErrorDetails(exception);
        String rootCauseMessage = Optional.ofNullable(exception.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse(exception.getMessage());
        log.warn(
                "Data integrity violation on [{}] mapped to field [{}]: {}",
                request.getRequestURI(),
                constraintErrorDetails.field(),
                rootCauseMessage,
                exception
        );
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                constraintErrorDetails.message(),
                List.of(ApiError.of(constraintErrorDetails.field(), constraintErrorDetails.detail())),
                request
        );
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handlePessimisticLockingFailureException(
            PessimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        log.warn("Lock acquisition timed out on [{}]: {}", request.getRequestURI(), exception.getMessage(), exception);
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "The requested resource is currently busy. Please retry shortly",
                List.of(ApiError.global("Another transaction is currently operating on the same resource")),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception exception, HttpServletRequest request) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                List.of(ApiError.global("An unexpected error occurred")),
                request
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String message,
            List<ApiError> errors,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(status)
                .body(ApiResponse.error(status, message, errors, request.getRequestURI()));
    }

    private ApiError toApiError(FieldError fieldError) {
        return ApiError.of(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ConstraintErrorDetails resolveConstraintErrorDetails(DataIntegrityViolationException exception) {
        String mostSpecificMessage = Optional.ofNullable(exception.getMostSpecificCause())
                .map(Throwable::getMessage)
                .orElse(exception.getMessage());
        String normalizedMessage = mostSpecificMessage == null ? "" : mostSpecificMessage.toLowerCase(Locale.ROOT);

        if (normalizedMessage.contains("alternative_phone_number")) {
            return new ConstraintErrorDetails(
                    "alternativePhoneNumber",
                    "Alternative phone number is already associated with another account",
                    "Alternative phone number must be unique"
            );
        }

        if (normalizedMessage.contains("phone_number")) {
            return new ConstraintErrorDetails(
                    "phoneNumber",
                    "Primary phone number is already associated with another account",
                    "Primary phone number must be unique"
            );
        }

        if (normalizedMessage.contains("email")) {
            return new ConstraintErrorDetails(
                    "email",
                    "Email address is already registered",
                    "Email address must be unique"
            );
        }

        if (normalizedMessage.contains("external_reference")) {
            return new ConstraintErrorDetails(
                    "reference",
                    "The supplied reference has already been used",
                    "Funding and transfer references must be unique per logical request"
            );
        }

        if (normalizedMessage.contains("wallet_type") || normalizedMessage.contains("user_id")) {
            return new ConstraintErrorDetails(
                    "walletType",
                    "You already have a wallet account of this type",
                    "Each user can only have one wallet per wallet type"
            );
        }

        if (normalizedMessage.contains("account_number")) {
            return new ConstraintErrorDetails(
                    "accountNumber",
                    "Account number already exists. Please retry the request",
                    "Generated account numbers must be unique"
            );
        }

        return new ConstraintErrorDetails(
                null,
                "Request conflicts with existing data",
                "The request could not be completed because a unique or relational constraint was violated"
        );
    }

    private record ConstraintErrorDetails(String field, String message, String detail) {
    }
}

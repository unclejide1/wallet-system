package com.example.test.common.exception;

import com.example.test.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void dataIntegrityViolationMapsAlternativePhoneConflictToFriendlyMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/onboard");

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("Unique index or primary key violation: \"UK_USERS_ALTERNATIVE_PHONE_NUMBER ON PUBLIC.USERS(ALTERNATIVE_PHONE_NUMBER)\"")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Alternative phone number is already associated with another account");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isEqualTo("alternativePhoneNumber");
    }

    @Test
    void dataIntegrityViolationFallsBackToGenericConflictWhenConstraintIsUnknown() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/wallets");

        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("some other integrity violation")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolationException(exception, request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Request conflicts with existing data");
        assertThat(response.getBody().getErrors()).hasSize(1);
        assertThat(response.getBody().getErrors().get(0).getField()).isNull();
    }
}

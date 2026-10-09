package com.honeygroup.honeylms.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unmappedUrl_returns404WithApiErrorBody() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/does-not-exist");

        ResponseEntity<ApiError> response = handler.handleNoResource(mock(NoResourceFoundException.class), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/api/does-not-exist");
        assertThat(response.getBody().message()).isEqualTo("No endpoint GET /api/does-not-exist");
    }
}

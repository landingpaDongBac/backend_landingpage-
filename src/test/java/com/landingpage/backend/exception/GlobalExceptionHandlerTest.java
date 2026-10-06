package com.landingpage.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsSafeLocalizedErrorForCloudinaryFailures() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/media/upload");

        var response = handler.multimediaUpload(
                new MultimediaUploadException("internal provider detail", new RuntimeException("provider failure")),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MULTIMEDIA_UPLOAD_FAILED");
        assertThat(response.getBody().message()).isEqualTo("Không thể tải tệp lên. Vui lòng thử lại sau.");
        assertThat(response.getBody().message()).doesNotContain("provider failure");
    }
}

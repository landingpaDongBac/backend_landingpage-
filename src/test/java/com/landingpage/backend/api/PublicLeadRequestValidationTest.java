package com.landingpage.backend.api;

import com.landingpage.backend.api.dto.PublicLeadRequest;
import com.landingpage.backend.domain.LeadSource;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicLeadRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsVietnameseMobileNumberAndExplicitConsent() {
        var request = new PublicLeadRequest("Nguyen Van A", "0901234567", "Coffee", null,
                null, null, LeadSource.CONSULTATION_FORM, true, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsNonVietnameseNumberAndMissingConsent() {
        var request = new PublicLeadRequest("Nguyen Van A", "1234567", "Coffee", null,
                null, null, LeadSource.CONSULTATION_FORM, false, null);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("phoneNumber", "consentAccepted");
    }
}

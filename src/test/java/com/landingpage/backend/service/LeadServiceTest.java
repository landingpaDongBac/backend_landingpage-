package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.PublicLeadRequest;
import com.landingpage.backend.api.dto.LeadStatusUpdateRequest;
import com.landingpage.backend.domain.Lead;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository repository;

    private LeadService service;

    @BeforeEach
    void setUp() {
        service = new LeadService(repository);
    }

    @Test
    void publicSubmissionForcesNewStatusAndTrimsFields() {
        when(repository.save(any(Lead.class))).thenAnswer(invocation -> {
            Lead lead = invocation.getArgument(0);
            lead.setId(UUID.randomUUID());
            lead.setCreatedAt(Instant.now());
            return lead;
        });
        var request = new PublicLeadRequest("  Nguyen Van A  ", " 0901234567 ", "  Coffee ", 100,
                "  2 ha ", "  Please call me ", LeadSource.CONSULTATION_FORM, true, null);

        var response = service.createPublic(request);

        assertThat(response.id()).isNotNull();
        org.mockito.Mockito.verify(repository).save(org.mockito.ArgumentMatchers.argThat(lead ->
                lead.getStatus() == LeadStatus.NEW
                        && lead.getFullName().equals("Nguyen Van A")
                        && lead.getPhoneNumber().equals("0901234567")));
    }

    @Test
    void honeypotSubmissionIsRejected() {
        var request = new PublicLeadRequest("Bot", "0901234567", "Coffee", null,
                null, null, LeadSource.OTHER, true, "spam.example");

        assertThatThrownBy(() -> service.createPublic(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void contactedStatusPreservesExistingHistoricalTimestamp() {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setStatus(LeadStatus.NEW);
        Instant historical = Instant.parse("2026-01-02T03:04:05Z");
        lead.setContactedAt(historical);
        when(repository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(repository.save(lead)).thenReturn(lead);

        service.updateStatus(lead.getId(), new LeadStatusUpdateRequest(LeadStatus.CONTACTED, null, null, null));

        assertThat(lead.getContactedAt()).isEqualTo(historical);
    }
}

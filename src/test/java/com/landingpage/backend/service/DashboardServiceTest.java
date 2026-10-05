package com.landingpage.backend.service;

import com.landingpage.backend.domain.ContentStatus;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.LeadRepository;
import com.landingpage.backend.repository.MediaAssetRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock LeadRepository leads;
    @Mock ContentDocumentRepository content;
    @Mock MediaAssetRepository media;

    @Test
    void aggregatesDatabaseCountsWithoutLoadingWholeTables() {
        when(leads.countByDeletedFalse()).thenReturn(12L);
        when(leads.countByDeletedFalseAndStatus(LeadStatus.NEW)).thenReturn(3L);
        when(leads.countPendingContact()).thenReturn(4L);
        when(content.countByStatus(ContentStatus.PUBLISHED)).thenReturn(5L);
        when(content.countByStatus(ContentStatus.DRAFT)).thenReturn(2L);
        when(media.count()).thenReturn(9L);
        when(leads.findTop5ByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of());
        when(content.findTop5ByOrderByUpdatedAtDesc()).thenReturn(List.of());

        var result = new DashboardService(leads, content, media).summary();

        assertThat(result.totalLeads()).isEqualTo(12);
        assertThat(result.pendingContact()).isEqualTo(4);
        assertThat(result.publishedDocuments()).isEqualTo(5);
        assertThat(result.totalMedia()).isEqualTo(9);
    }
}

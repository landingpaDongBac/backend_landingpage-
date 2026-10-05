package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.DashboardSummaryResponse;
import com.landingpage.backend.domain.ContentStatus;
import com.landingpage.backend.domain.LeadStatus;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.LeadRepository;
import com.landingpage.backend.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final LeadRepository leadRepository;
    private final ContentDocumentRepository contentRepository;
    private final MediaAssetRepository mediaRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        var recentLeads = leadRepository.findTop5ByDeletedFalseOrderByCreatedAtDesc().stream()
                .map(lead -> new DashboardSummaryResponse.RecentLead(lead.getId(), lead.getFullName(),
                        lead.getPhoneNumber(), lead.getCropType(), lead.getSource(), lead.getStatus(), lead.getCreatedAt()))
                .toList();
        var recentContent = contentRepository.findTop5ByOrderByUpdatedAtDesc().stream()
                .map(document -> new DashboardSummaryResponse.RecentContent(document.getId(), document.getTitle(),
                        document.getSlug(), document.getSection().getSectionKey(), document.getStatus(),
                        document.getUpdatedAt(), document.getUpdatedBy()))
                .toList();
        return new DashboardSummaryResponse(
                leadRepository.countByDeletedFalse(),
                leadRepository.countByDeletedFalseAndStatus(LeadStatus.NEW),
                leadRepository.countPendingContact(),
                contentRepository.countByStatus(ContentStatus.PUBLISHED),
                contentRepository.countByStatus(ContentStatus.DRAFT),
                mediaRepository.count(),
                recentLeads,
                recentContent);
    }
}

package com.landingpage.backend.api.dto;

import com.landingpage.backend.domain.ContentStatus;
import com.landingpage.backend.domain.LeadSource;
import com.landingpage.backend.domain.LeadStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardSummaryResponse(
        long totalLeads,
        long newLeads,
        long pendingContact,
        long publishedDocuments,
        long draftDocuments,
        long totalMedia,
        List<RecentLead> recentLeads,
        List<RecentContent> recentContentUpdates
) {
    public record RecentLead(UUID id, String fullName, String phoneNumber, String cropType,
                             LeadSource source, LeadStatus status, Instant createdAt) { }
    public record RecentContent(UUID id, String title, String slug, String sectionKey,
                                ContentStatus status, Instant updatedAt, String updatedBy) { }
}

package com.landingpage.backend.repository;

import com.landingpage.backend.domain.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;
import java.util.List;

import com.landingpage.backend.domain.LeadStatus;
import org.springframework.data.jpa.repository.Query;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
    long countByDeletedFalse();
    long countByDeletedFalseAndStatus(LeadStatus status);
    List<Lead> findTop5ByDeletedFalseOrderByCreatedAtDesc();

    @Query("""
            select count(l) from Lead l
            where l.deleted = false and (
              (l.contactedAt is null and l.status in (com.landingpage.backend.domain.LeadStatus.NEW,
                com.landingpage.backend.domain.LeadStatus.QUALIFIED))
              or (l.followUpAt is not null and l.followUpAt <= CURRENT_TIMESTAMP
                and l.status not in (com.landingpage.backend.domain.LeadStatus.CONVERTED,
                  com.landingpage.backend.domain.LeadStatus.CLOSED))
            )
            """)
    long countPendingContact();
}

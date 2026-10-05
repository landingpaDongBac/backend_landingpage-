package com.landingpage.backend.service;

import com.landingpage.backend.api.dto.SectionOrderRequest;
import com.landingpage.backend.domain.Section;
import com.landingpage.backend.exception.BadRequestException;
import com.landingpage.backend.repository.ContentDocumentRepository;
import com.landingpage.backend.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SectionServiceTest {
    @Mock SectionRepository sections;
    @Mock ContentDocumentRepository content;

    @Test
    void rejectsDuplicateDisplayOrders() {
        Section first = section("02");
        Section second = section("03");
        when(sections.findAll()).thenReturn(List.of(first, second));
        var request = new SectionOrderRequest(List.of(
                new SectionOrderRequest.Item(first.getId(), 1),
                new SectionOrderRequest.Item(second.getId(), 1)));

        assertThatThrownBy(() -> new SectionService(sections, content).reorder(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate section display order");
    }

    private Section section(String key) {
        Section section = new Section();
        section.setId(UUID.randomUUID());
        section.setSectionKey(key);
        return section;
    }
}

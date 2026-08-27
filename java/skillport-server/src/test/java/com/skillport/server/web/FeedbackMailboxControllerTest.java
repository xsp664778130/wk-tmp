package com.skillport.server.web;

import com.skillport.server.domain.FeedbackMessageEntity;
import com.skillport.server.service.FeedbackMailboxService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedbackMailboxControllerTest {
    @Test
    void exposesSubmitterContentTimeAndPaginationWithoutOwnerIdentity() {
        FeedbackMailboxService service = mock(FeedbackMailboxService.class);
        Instant createdAt = Instant.parse("2026-08-25T08:30:00Z");
        var message = new FeedbackMessageEntity(
                "feedback-1", "private-owner-id", "小明", "功能建议", "希望支持公开意见墙", createdAt);
        when(service.list(2, 6)).thenReturn(new PageImpl<>(
                List.of(message), PageRequest.of(1, 6), 13));

        var response = new FeedbackMailboxController(service).list(2, 6);

        assertEquals(2, response.page());
        assertEquals(3, response.totalPages());
        assertEquals(13, response.totalElements());
        assertTrue(response.hasPrevious());
        assertTrue(response.hasNext());
        assertEquals("小明", response.items().getFirst().submitter());
        assertEquals("希望支持公开意见墙", response.items().getFirst().content());
        assertEquals(createdAt, response.items().getFirst().createdAt());
    }
}

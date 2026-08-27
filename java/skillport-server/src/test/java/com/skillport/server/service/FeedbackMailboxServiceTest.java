package com.skillport.server.service;

import com.skillport.server.domain.FeedbackMessageEntity;
import com.skillport.server.repository.FeedbackMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackMailboxServiceTest {
    @Test
    void storesTrimmedFeedbackForTheAuthenticatedOwner() {
        FeedbackMessageRepository repository = mock(FeedbackMessageRepository.class);
        when(repository.save(any(FeedbackMessageEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        FeedbackMailboxService service = new FeedbackMailboxService(repository);

        FeedbackMessageEntity message = service.submit(
                "owner-1", "  小明  ", "功能建议", "  希望支持意见信箱  ");

        assertEquals("owner-1", message.getOwnerId());
        assertEquals("小明", message.getSubmitterDisplayName());
        assertEquals("功能建议", message.getKind());
        assertEquals("希望支持意见信箱", message.getContent());
        assertEquals("NEW", message.getStatus());
        verify(repository).save(message);
    }

    @Test
    void rejectsFeedbackThatIsTooShort() {
        FeedbackMailboxService service = new FeedbackMailboxService(mock(FeedbackMessageRepository.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.submit("owner-1", "小明", "问题反馈", "太短"));

        assertEquals(400, exception.getStatusCode().value());
    }

    @Test
    void returnsNewestPublicFeedbackWithBoundedServerSidePagination() {
        FeedbackMessageRepository repository = mock(FeedbackMessageRepository.class);
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));
        FeedbackMailboxService service = new FeedbackMailboxService(repository);

        service.list(2, 1000);

        var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals("createdAt: DESC,id: DESC", pageable.getValue().getSort().toString());
    }
}

package com.skillport.server.web;

import com.skillport.server.domain.FeedbackMessageEntity;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.FeedbackMailboxService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackMailboxController {
    private final FeedbackMailboxService service;

    public FeedbackMailboxController(FeedbackMailboxService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse submit(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @Valid @RequestBody FeedbackRequest request) {
        return FeedbackResponse.from(service.submit(
                user.userId(), user.displayName(), request.kind(), request.content()));
    }

    @GetMapping
    public FeedbackPageResponse list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size) {
        var result = service.list(page, size);
        return new FeedbackPageResponse(
                result.getContent().stream().map(PublicFeedbackResponse::from).toList(),
                result.getNumber() + 1,
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasPrevious(),
                result.hasNext());
    }

    public record FeedbackRequest(
            @Size(max = 32) String kind,
            @NotBlank @Size(min = 5, max = 2000) String content) {
    }

    public record FeedbackResponse(String id, String kind, String status, Instant createdAt) {
        static FeedbackResponse from(FeedbackMessageEntity message) {
            return new FeedbackResponse(
                    message.getPublicId(), message.getKind(), message.getStatus(), message.getCreatedAt());
        }
    }

    public record PublicFeedbackResponse(
            String id, String submitter, String kind, String content, Instant createdAt) {
        static PublicFeedbackResponse from(FeedbackMessageEntity message) {
            return new PublicFeedbackResponse(
                    message.getPublicId(), message.getSubmitterDisplayName(), message.getKind(),
                    message.getContent(), message.getCreatedAt());
        }
    }

    public record FeedbackPageResponse(
            List<PublicFeedbackResponse> items,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasPrevious,
            boolean hasNext) {
    }
}

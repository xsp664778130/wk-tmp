package com.skillport.server.web;

import com.skillport.server.domain.PublicSkillEntity;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.PublicSkillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public-skills")
public class PublicSkillController {
    private final PublicSkillService publicSkillService;

    public PublicSkillController(PublicSkillService publicSkillService) {
        this.publicSkillService = publicSkillService;
    }

    @GetMapping
    public PublicSkillListResponse list(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        return new PublicSkillListResponse(publicSkillService.list(user.userId()).stream()
                .map(PublicSkillResponse::from)
                .toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicSkillResponse share(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @Valid @RequestBody ShareRequest request) {
        return PublicSkillResponse.from(publicSkillService.share(
                user.userId(), user.displayName(), request.skillId()), true);
    }

    @PostMapping("/{publicSkillId}/pull")
    public PullResponse pull(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String publicSkillId) {
        PublicSkillService.PullResult result = publicSkillService.pull(user.userId(), publicSkillId);
        return new PullResponse(SkillController.SkillResponse.from(result.skill()), result.created());
    }

    public record ShareRequest(@NotBlank String skillId) {
    }

    public record PublicSkillListResponse(List<PublicSkillResponse> skills) {
    }

    public record PullResponse(SkillController.SkillResponse skill, boolean created) {
    }

    public record PublicSkillResponse(String id, String name, String description, String category,
                                      String fileName, long sizeBytes, String sha256,
                                      List<String> compatible, String author, long pullCount,
                                      Instant publishedAt, boolean pulled) {
        static PublicSkillResponse from(PublicSkillService.PublicSkillView view) {
            return from(view.publication(), view.pulled());
        }

        static PublicSkillResponse from(PublicSkillEntity publication, boolean pulled) {
            return new PublicSkillResponse(publication.getPublicId(), publication.getName(),
                    publication.getDescription(), publication.getCategory(), publication.getFileName(),
                    publication.getSizeBytes(), publication.getSha256(),
                    Arrays.stream(publication.getToolCompatibility().split(",")).toList(),
                    publication.getPublisherDisplayName(), publication.getPullCount(),
                    publication.getPublishedAt(), pulled);
        }
    }
}

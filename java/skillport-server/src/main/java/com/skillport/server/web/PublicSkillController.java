package com.skillport.server.web;

import com.skillport.server.domain.PublicSkillEntity;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.PublicSkillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import java.io.IOException;
import java.nio.file.Path;
import com.skillport.server.domain.SkillEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import com.skillport.server.storage.FileStorageService;
import com.skillport.server.service.SkillPackageEnvironmentService;

@RestController
@RequestMapping("/api/v1/public-skills")
public class PublicSkillController {
    private final PublicSkillService publicSkillService;
    private final FileStorageService fileStorageService;
    private final SkillPackageEnvironmentService environmentService;

    public PublicSkillController(PublicSkillService publicSkillService, FileStorageService fileStorageService,
                                 SkillPackageEnvironmentService environmentService) {
        this.publicSkillService = publicSkillService;
        this.fileStorageService = fileStorageService;
        this.environmentService = environmentService;
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
        PublicSkillEntity publication = publicSkillService.share(
                user.userId(), user.displayName(), request.skillId());
        return PublicSkillResponse.from(publication, true,
                publicSkillService.publicAvatarAvailable(publication.getPublicId()));
    }

    @PostMapping("/{publicSkillId}/pull")
    public PullResponse pull(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String publicSkillId) {
        PublicSkillService.PullResult result = publicSkillService.pull(user.userId(), publicSkillId);
        return new PullResponse(SkillController.SkillResponse.from(result.skill()), result.created());
    }

    @DeleteMapping("/{publicSkillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpublish(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
                          @PathVariable String publicSkillId) {
        publicSkillService.unpublish(user.userId(), publicSkillId);
    }

    @DeleteMapping("/source/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpublishBySource(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
                                  @PathVariable String skillId) {
        publicSkillService.unpublishBySource(user.userId(), skillId);
    }

    @GetMapping("/{publicSkillId}/avatar")
    public ResponseEntity<InputStreamResource> avatar(@PathVariable String publicSkillId) throws IOException {
        SkillEntity source = publicSkillService.publicAvatarSource(publicSkillId);
        Path file = fileStorageService.resolve(source.getAvatarStoragePath());
        return SkillController.avatarResponse(source, file);
    }

    @GetMapping("/{publicSkillId}/environment")
    public SkillController.SkillEnvironmentResponse environment(@PathVariable String publicSkillId) {
        SkillEntity source = publicSkillService.publicSkillSource(publicSkillId);
        try {
            return SkillController.SkillEnvironmentResponse.from(
                    environmentService.read(fileStorageService.resolve(source.getStoragePath()), source.getFileName()),
                    false);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), exception);
        }
    }

    public record ShareRequest(@NotBlank String skillId) {
    }

    public record PublicSkillListResponse(List<PublicSkillResponse> skills) {
    }

    public record PullResponse(SkillController.SkillResponse skill, boolean created) {
    }

    public record PublicSkillResponse(String id, String name, String description, String detail,
                                      List<String> usageSteps, String category,
                                      String fileName, long sizeBytes, String sha256,
                                      List<String> compatible, String author, long pullCount,
                                      Instant publishedAt, boolean pulled, boolean ownedByCurrentUser,
                                      String sourceSkillId, String avatarUrl) {
        static PublicSkillResponse from(PublicSkillService.PublicSkillView view) {
            PublicSkillEntity publication = view.publication();
            return new PublicSkillResponse(publication.getPublicId(), publication.getName(),
                    publication.getDescription(), publication.getDetail(),
                    SkillController.usageStepList(publication.getUsageSteps()), publication.getCategory(),
                    publication.getFileName(),
                    publication.getSizeBytes(), publication.getSha256(),
                    Arrays.stream(publication.getToolCompatibility().split(",")).toList(),
                    publication.getPublisherDisplayName(), publication.getPullCount(),
                    publication.getPublishedAt(), view.pulled(), view.ownedByCurrentUser(),
                    publication.getSourceSkillPublicId(),
                    view.hasAvatar() ? "/api/public-skills/" + publication.getPublicId() + "/avatar" : null);
        }

        static PublicSkillResponse from(PublicSkillEntity publication, boolean pulled, boolean hasAvatar) {
            return new PublicSkillResponse(publication.getPublicId(), publication.getName(),
                    publication.getDescription(), publication.getDetail(),
                    SkillController.usageStepList(publication.getUsageSteps()), publication.getCategory(),
                    publication.getFileName(),
                    publication.getSizeBytes(), publication.getSha256(),
                    Arrays.stream(publication.getToolCompatibility().split(",")).toList(),
                    publication.getPublisherDisplayName(), publication.getPullCount(),
                    publication.getPublishedAt(), pulled, pulled,
                    publication.getSourceSkillPublicId(),
                    hasAvatar ? "/api/public-skills/" + publication.getPublicId() + "/avatar" : null);
        }
    }
}

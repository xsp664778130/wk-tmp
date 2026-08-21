package com.skillport.server.web;

import com.skillport.server.domain.SkillEntity;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.RequestUserFilter;
import com.skillport.server.service.PublicSkillService;
import com.skillport.server.service.SkillService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {
    private final SkillService skillService;
    private final PublicSkillService publicSkillService;

    public SkillController(SkillService skillService, PublicSkillService publicSkillService) {
        this.skillService = skillService;
        this.publicSkillService = publicSkillService;
    }

    @GetMapping
    public SkillListResponse list(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user) {
        Set<String> sharedSkillIds = publicSkillService.sharedSourceSkillIds(user.userId());
        return new SkillListResponse(skillService.list(user.userId()).stream()
                .map(skill -> SkillResponse.from(skill, sharedSkillIds.contains(skill.getPublicId())))
                .toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse upload(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
                                @RequestParam String name,
                                @RequestParam(defaultValue = "") String description,
                                @RequestParam(defaultValue = "编程开发") String category,
                                @RequestPart MultipartFile file) {
        return SkillResponse.from(skillService.upload(user.userId(), name, description, category, file));
    }

    @PatchMapping("/{skillId}/note")
    public SkillResponse note(@RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
                              @PathVariable String skillId,
                              @Valid @RequestBody NoteRequest request) {
        return SkillResponse.from(skillService.updateNote(user.userId(), skillId, request.note()));
    }

    @GetMapping("/{skillId}/content")
    public ResponseEntity<InputStreamResource> content(
            @RequestAttribute(RequestUserFilter.REQUEST_USER_ATTRIBUTE) RequestUser user,
            @PathVariable String skillId) throws IOException {
        SkillEntity skill = skillService.ownedSkill(user.userId(), skillId);
        Path file = skillService.ownedFile(user.userId(), skillId);
        InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(skill.getContentType()))
                .contentLength(skill.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(skill.getFileName()).build().toString())
                .header("X-Skill-Extension", fileExtension(skill.getFileName()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(resource);
    }

    static String fileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "zip" : fileName.substring(dot + 1).toLowerCase();
        return extension.equals("zip") || extension.equals("skill") ? extension : "md";
    }

    public record NoteRequest(@Size(max = 2000) String note) {
    }
    public record SkillListResponse(List<SkillResponse> skills) {
    }
    public record SkillResponse(String id, String name, String description, String category, String fileName,
                                long sizeBytes, String sha256, String note, String toolCompatibility,
                                boolean shared, String sourcePublicSkillId,
                                Instant createdAt, Instant updatedAt) {
        static SkillResponse from(SkillEntity skill) {
            return from(skill, false);
        }

        static SkillResponse from(SkillEntity skill, boolean shared) {
            return new SkillResponse(skill.getPublicId(), skill.getName(), skill.getDescription(), skill.getCategory(),
                    skill.getFileName(), skill.getSizeBytes(), skill.getSha256(), skill.getNote(),
                    "codex,qoder,openai", shared, skill.getSourcePublicSkillId(),
                    skill.getCreatedAt(), skill.getUpdatedAt());
        }
    }
}

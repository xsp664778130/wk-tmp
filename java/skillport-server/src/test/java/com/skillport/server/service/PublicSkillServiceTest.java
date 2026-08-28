package com.skillport.server.service;

import com.skillport.server.domain.PublicSkillEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.PublicSkillRepository;
import com.skillport.server.repository.SkillRepository;
import com.skillport.server.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class PublicSkillServiceTest {
    @Autowired
    private SkillService skillService;
    @Autowired
    private PublicSkillService publicSkillService;
    @Autowired
    private PublicSkillRepository publicSkillRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void sharesPublicMetadataAndPullsAnIdempotentPrivateCopyWithoutTheNote() throws Exception {
        String publisherId = "publisher-" + UUID.randomUUID();
        String readerId = "reader-" + UUID.randomUUID();
        byte[] content = skillManifest("release-guard");
        MockMultipartFile file = new MockMultipartFile(
                "file", "SKILL.md", "text/markdown", content);

        SkillEntity source = skillService.upload(
                publisherId, "Release Guard", "Checks a release", "测试工具", file);
        skillService.updateNote(publisherId, source.getPublicId(), "internal project only");

        PublicSkillEntity publication = publicSkillService.share(
                publisherId, "Publisher", source.getPublicId());
        SkillService.CategoryUpdateResult categoryUpdate = skillService.updateCategory(
                publisherId, source.getPublicId(), "日志技能");
        assertTrue(categoryUpdate.publicPoolSynchronized());
        assertEquals("日志技能", categoryUpdate.skill().getCategory());
        assertEquals("日志技能", publicSkillRepository.findByPublicId(publication.getPublicId())
                .orElseThrow().getCategory());
        SkillService.DetailUpdateResult detailUpdate = skillService.updateDetails(
                publisherId,
                source.getPublicId(),
                "Release Guard Pro",
                "Checks every release gate",
                "Validate configuration, dependencies and deployment readiness before publishing.",
                List.of("Select the target project", "Run all release checks", "Review the generated report"));
        assertTrue(detailUpdate.publicPoolSynchronized());
        PublicSkillService.PublicSkillView listed = publicSkillService.list(readerId).stream()
                .filter(view -> view.publication().getPublicId().equals(publication.getPublicId()))
                .findFirst()
                .orElseThrow();
        assertFalse(listed.pulled());
        assertEquals("Publisher", listed.publication().getPublisherDisplayName());
        assertEquals("Release Guard Pro", listed.publication().getName());
        assertEquals("Checks every release gate", listed.publication().getDescription());
        assertEquals("Validate configuration, dependencies and deployment readiness before publishing.",
                listed.publication().getDetail());
        assertEquals("Select the target project\nRun all release checks\nReview the generated report",
                listed.publication().getUsageSteps());
        assertEquals("日志技能", listed.publication().getCategory());

        PublicSkillService.PullResult firstPull = publicSkillService.pull(readerId, publication.getPublicId());
        SkillEntity imported = firstPull.skill();
        assertTrue(firstPull.created());
        assertNotEquals(source.getPublicId(), imported.getPublicId());
        assertEquals(readerId, imported.getOwnerId());
        assertEquals("", imported.getNote());
        assertEquals(publication.getPublicId(), imported.getSourcePublicSkillId());
        assertEquals("日志技能", imported.getCategory());
        assertEquals(listed.publication().getDetail(), imported.getDetail());
        assertEquals(listed.publication().getUsageSteps(), imported.getUsageSteps());
        assertEquals(source.getSha256(), imported.getSha256());
        assertArrayEquals(content, Files.readAllBytes(fileStorageService.resolve(imported.getStoragePath())));

        PublicSkillService.PullResult repeatedPull = publicSkillService.pull(readerId, publication.getPublicId());
        assertFalse(repeatedPull.created());
        assertEquals(imported.getPublicId(), repeatedPull.skill().getPublicId());
        assertEquals(1, publicSkillRepository.findByPublicId(publication.getPublicId()).orElseThrow().getPullCount());
        assertTrue(publicSkillService.list(readerId).stream()
                .filter(view -> view.publication().getPublicId().equals(publication.getPublicId()))
                .findFirst().orElseThrow().pulled());
        assertEquals(Set.of(source.getPublicId()), publicSkillService.sharedSourceSkillIds(publisherId));
    }

    @Test
    void refusesToShareAnotherUsersPrivateSkill() {
        String ownerId = "owner-" + UUID.randomUUID();
        String attackerId = "attacker-" + UUID.randomUUID();
        SkillEntity source = skillService.upload(ownerId, "Private", "Not shared", "编程开发",
                new MockMultipartFile("file", "SKILL.md", "text/markdown",
                        skillManifest("private-skill")));

        try {
            publicSkillService.share(attackerId, "Attacker", source.getPublicId());
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            assertEquals(404, exception.getStatusCode().value());
            return;
        }
        throw new AssertionError("Sharing another user's private Skill must fail");
    }

    @Test
    void copiesAvatarAndOnlyPublisherCanUnpublish() throws Exception {
        String publisherId = "avatar-publisher-" + UUID.randomUUID();
        String readerId = "avatar-reader-" + UUID.randomUUID();
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2, 3};
        SkillEntity source = skillService.upload(publisherId, "Avatar Skill", "Has an avatar", "编程技能",
                new MockMultipartFile("file", "SKILL.md", "text/markdown", skillManifest("avatar-skill")),
                new MockMultipartFile("avatar", "avatar.png", "image/png", png));
        Path originalAvatar = fileStorageService.resolve(source.getAvatarStoragePath());
        PublicSkillEntity publication = publicSkillService.share(publisherId, "Publisher", source.getPublicId());

        PublicSkillService.PublicSkillView view = publicSkillService.list(readerId).stream()
                .filter(candidate -> candidate.publication().getPublicId().equals(publication.getPublicId()))
                .findFirst().orElseThrow();
        assertTrue(view.hasAvatar());
        assertFalse(view.ownedByCurrentUser());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> publicSkillService.unpublish(readerId, publication.getPublicId()));

        SkillEntity imported = publicSkillService.pull(readerId, publication.getPublicId()).skill();
        assertTrue(imported.hasAvatar());
        assertArrayEquals(png, Files.readAllBytes(fileStorageService.resolve(imported.getAvatarStoragePath())));

        byte[] jpeg = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 7, 8, 9};
        SkillService.AvatarUpdateResult avatarUpdate = skillService.updateAvatar(
                publisherId, source.getPublicId(),
                new MockMultipartFile("avatar", "replacement.jpg", "image/jpeg", jpeg));
        assertTrue(avatarUpdate.publicPoolSynchronized());
        assertArrayEquals(jpeg, Files.readAllBytes(publicSkillService.publicAvatarFile(publication.getPublicId())));
        assertFalse(Files.exists(originalAvatar));
        assertArrayEquals(png, Files.readAllBytes(fileStorageService.resolve(imported.getAvatarStoragePath())));

        Path replacementAvatar = publicSkillService.publicAvatarFile(publication.getPublicId());
        SkillService.AvatarUpdateResult avatarRemoval = skillService.removeAvatar(publisherId, source.getPublicId());
        assertTrue(avatarRemoval.publicPoolSynchronized());
        assertFalse(avatarRemoval.skill().hasAvatar());
        assertFalse(publicSkillService.publicAvatarAvailable(publication.getPublicId()));
        assertFalse(Files.exists(replacementAvatar));

        publicSkillService.unpublish(publisherId, publication.getPublicId());
        assertTrue(publicSkillRepository.findByPublicId(publication.getPublicId()).isEmpty());
        assertTrue(skillRepository.findByPublicId(source.getPublicId()).isPresent());
    }

    @Test
    void deletingOwnedSkillAlsoRemovesPublicationAndFiles() {
        String ownerId = "delete-owner-" + UUID.randomUUID();
        SkillEntity source = skillService.upload(ownerId, "Delete Me", "Temporary", "排查技能",
                new MockMultipartFile("file", "SKILL.md", "text/markdown",
                        skillManifest("delete-me")));
        Path storedFile = fileStorageService.resolve(source.getStoragePath());
        PublicSkillEntity publication = publicSkillService.share(ownerId, "Owner", source.getPublicId());

        skillService.deleteOwned(ownerId, source.getPublicId());

        assertTrue(skillRepository.findByPublicId(source.getPublicId()).isEmpty());
        assertTrue(publicSkillRepository.findByPublicId(publication.getPublicId()).isEmpty());
        assertFalse(Files.exists(storedFile));
    }

    private static byte[] skillManifest(String name) {
        return ("---\nname: " + name + "\ndescription: Test Skill\n---\n\n"
                + "# Instructions\n\nRun the requested test workflow.\n").getBytes(StandardCharsets.UTF_8);
    }
}

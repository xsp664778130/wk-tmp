package com.skillport.server.service;

import com.skillport.server.domain.PublicSkillEntity;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.repository.PublicSkillRepository;
import com.skillport.server.storage.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private FileStorageService fileStorageService;

    @Test
    void sharesPublicMetadataAndPullsAnIdempotentPrivateCopyWithoutTheNote() throws Exception {
        String publisherId = "publisher-" + UUID.randomUUID();
        String readerId = "reader-" + UUID.randomUUID();
        byte[] content = "# Shared Skill\n\nPublic instructions.".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "SKILL.md", "text/markdown", content);

        SkillEntity source = skillService.upload(
                publisherId, "Release Guard", "Checks a release", "测试工具", file);
        skillService.updateNote(publisherId, source.getPublicId(), "internal project only");

        PublicSkillEntity publication = publicSkillService.share(
                publisherId, "Publisher", source.getPublicId());
        PublicSkillService.PublicSkillView listed = publicSkillService.list(readerId).stream()
                .filter(view -> view.publication().getPublicId().equals(publication.getPublicId()))
                .findFirst()
                .orElseThrow();
        assertFalse(listed.pulled());
        assertEquals("Publisher", listed.publication().getPublisherDisplayName());
        assertEquals("Release Guard", listed.publication().getName());

        PublicSkillService.PullResult firstPull = publicSkillService.pull(readerId, publication.getPublicId());
        SkillEntity imported = firstPull.skill();
        assertTrue(firstPull.created());
        assertNotEquals(source.getPublicId(), imported.getPublicId());
        assertEquals(readerId, imported.getOwnerId());
        assertEquals("", imported.getNote());
        assertEquals(publication.getPublicId(), imported.getSourcePublicSkillId());
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
                        "private".getBytes(StandardCharsets.UTF_8)));

        try {
            publicSkillService.share(attackerId, "Attacker", source.getPublicId());
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            assertEquals(404, exception.getStatusCode().value());
            return;
        }
        throw new AssertionError("Sharing another user's private Skill must fail");
    }
}

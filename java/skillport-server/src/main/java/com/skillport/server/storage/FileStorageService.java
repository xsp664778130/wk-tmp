package com.skillport.server.storage;

import com.skillport.server.config.SkillPortProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.util.HexFormat;

@Service
public class FileStorageService {
    private final Path storageRoot;

    public FileStorageService(SkillPortProperties properties) {
        this.storageRoot = properties.storageRoot().toAbsolutePath().normalize();
    }

    public StoredSkillFile store(String ownerId, String skillId, String originalFilename, InputStream source) {
        String safeOwner = safeSegment(ownerId);
        String safeFilename = safeFilename(originalFilename);
        Path target = storageRoot.resolve(safeOwner).resolve(skillId).resolve(safeFilename).normalize();
        if (!target.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid storage path");
        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digestInput = new DigestInputStream(source, digest)) {
                Files.copy(digestInput, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredSkillFile(target, Files.size(target), HexFormat.of().formatHex(digest.digest()));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store skill file", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public Path resolve(String storagePath) {
        Path path = Path.of(storagePath).toAbsolutePath().normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Skill file is unavailable");
        }
        return path;
    }

    public StoredSkillFile copy(String ownerId, String skillId, String originalFilename, String sourceStoragePath) {
        Path source = resolve(sourceStoragePath);
        try (InputStream input = Files.newInputStream(source)) {
            return store(ownerId, skillId, originalFilename, input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to copy shared skill file", exception);
        }
    }

    private static String safeSegment(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String safeFilename(String value) {
        String filename = Path.of(value == null ? "skill.zip" : value).getFileName().toString();
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

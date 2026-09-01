package com.skillport.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.server.domain.SkillEntity;
import com.skillport.server.domain.UserEntity;
import com.skillport.server.repository.SkillRepository;
import com.skillport.server.repository.UserRepository;
import com.skillport.server.service.PasswordResetStore;
import com.skillport.server.storage.FileStorageService;
import com.skillport.server.storage.StoredSkillFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SkillPortServerApplicationTest {
    private static final String GATEWAY_KEY = "test-gateway-key";

    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SkillRepository skillRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private PasswordResetStore passwordResetStore;
    @Autowired
    private FileStorageService fileStorageService;

    @Test
    void contextLoadsWithMySqlCompatibleSchemaAndNetty() {
    }

    @Test
    void registersAuthenticatesAndRevokesDatabaseUserSession() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@example.com";
        String password = "StrongPass-2026";
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> register = client.send(HttpRequest.newBuilder(api("/api/v1/auth/register"))
                        .header("Content-Type", "application/json")
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Test Owner", password))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, register.statusCode());
        JsonNode registered = objectMapper.readTree(register.body());
        String token = registered.path("token").asText();
        String userId = registered.path("user").path("id").asText();
        assertTrue(token.length() >= 40);

        UserEntity storedUser = userRepository.findByEmailNormalized(email).orElseThrow();
        assertNotEquals(password, storedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, storedUser.getPasswordHash()));

        HttpResponse<String> anonymousSkills = client.send(HttpRequest.newBuilder(api("/api/v1/skills"))
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, anonymousSkills.statusCode());

        HttpResponse<String> me = client.send(authenticated(api("/api/v1/auth/me"), token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, me.statusCode());
        assertEquals(userId, objectMapper.readTree(me.body()).path("id").asText());

        HttpResponse<String> logout = client.send(authenticated(api("/api/v1/auth/logout"), token)
                        .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, logout.statusCode());

        HttpResponse<String> afterLogout = client.send(authenticated(api("/api/v1/auth/me"), token).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(401, afterLogout.statusCode());
    }

    @Test
    void servesStaticWorkspaceAndKeepsBrowserSessionInHttpOnlyCookie() throws Exception {
        String email = "browser-" + UUID.randomUUID() + "@example.com";
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> home = client.send(HttpRequest.newBuilder(api("/"))
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, home.statusCode());
        assertTrue(home.body().contains("SkillPort — AI Skill 管理工作台"));

        HttpResponse<String> register = client.send(HttpRequest.newBuilder(api("/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Browser Owner", "StrongPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, register.statusCode());
        assertTrue(register.headers().firstValue("set-cookie").orElseThrow().contains("HttpOnly"));
        assertEquals("", objectMapper.readTree(register.body()).path("token").asText());

        HttpResponse<String> duplicate = client.send(HttpRequest.newBuilder(api("/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Browser Owner", "StrongPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(409, duplicate.statusCode());
        assertEquals("该邮箱已经注册", objectMapper.readTree(duplicate.body()).path("error").asText());

        String cookie = register.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        HttpResponse<String> me = client.send(HttpRequest.newBuilder(api("/api/auth/me"))
                        .header("Cookie", cookie)
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, me.statusCode());
        assertEquals(email, objectMapper.readTree(me.body()).path("user").path("email").asText());

        HttpResponse<String> logout = client.send(HttpRequest.newBuilder(api("/api/auth/logout"))
                        .header("Cookie", cookie)
                        .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, logout.statusCode());

        HttpResponse<String> afterLogout = client.send(HttpRequest.newBuilder(api("/api/auth/me"))
                        .header("Cookie", cookie)
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, afterLogout.statusCode());
    }

    @Test
    void updatesProfileAndRevokesSessionsAfterChangingPassword() throws Exception {
        String email = "profile-" + UUID.randomUUID() + "@example.com";
        String oldPassword = "StrongPass-2026";
        String newPassword = "ChangedPass-2026";
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> register = client.send(HttpRequest.newBuilder(api("/api/v1/auth/register"))
                        .header("Content-Type", "application/json")
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Before Name", oldPassword))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        String token = objectMapper.readTree(register.body()).path("token").asText();

        HttpResponse<String> profile = client.send(authenticated(api("/api/v1/auth/profile"), token)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"displayName\":\"After Name\"}"))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, profile.statusCode());
        assertEquals("After Name", objectMapper.readTree(profile.body()).path("displayName").asText());
        assertTrue(objectMapper.readTree(profile.body()).path("passwordEnabled").asBoolean());

        HttpResponse<String> change = client.send(authenticated(api("/api/v1/auth/password/change"), token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                                new PasswordChange(oldPassword, newPassword))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, change.statusCode());
        assertEquals(401, client.send(authenticated(api("/api/v1/auth/me"), token).GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(200, login(client, email, newPassword).statusCode());
    }

    @Test
    void resetsPasswordWithOneTimeEmailCodeAndRevokesExistingSessions() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> register = client.send(HttpRequest.newBuilder(api("/api/v1/auth/register"))
                        .header("Content-Type", "application/json")
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Reset Owner", "StrongPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        String token = objectMapper.readTree(register.body()).path("token").asText();
        PasswordResetStore.IssuedCode issued = passwordResetStore.issueCode(email);

        HttpResponse<String> reset = client.send(HttpRequest.newBuilder(api("/api/v1/auth/password/reset"))
                        .header("Content-Type", "application/json")
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                                new PasswordReset(email, issued.code(), "ResetPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(204, reset.statusCode());
        assertEquals(401, client.send(authenticated(api("/api/v1/auth/me"), token).GET().build(),
                HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(200, login(client, email, "ResetPass-2026").statusCode());

        HttpResponse<String> reused = client.send(HttpRequest.newBuilder(api("/api/v1/auth/password/reset"))
                        .header("Content-Type", "application/json")
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                                new PasswordReset(email, issued.code(), "AnotherPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, reused.statusCode());
    }

    @Test
    void updatesCategoryAndDetailsThroughTheWebClientPatchRoute() throws Exception {
        String email = "patch-" + UUID.randomUUID() + "@example.com";
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> register = client.send(HttpRequest.newBuilder(api("/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Patch Owner", "StrongPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, register.statusCode());

        JsonNode registered = objectMapper.readTree(register.body());
        String ownerId = registered.path("user").path("id").asText();
        String cookie = register.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        String skillId = UUID.randomUUID().toString();
        skillRepository.save(new SkillEntity(
                skillId, ownerId, "Before", "Before description", "编程技能", "sample.zip",
                ownerId + "/" + skillId + "/sample.zip", "application/zip", 1L, "0".repeat(64),
                Instant.now()));

        HttpResponse<String> category = client.send(HttpRequest.newBuilder(api("/api/skills/" + skillId))
                        .header("Cookie", cookie)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(new CategoryPatch("排查技能"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, category.statusCode());
        assertEquals("排查技能", objectMapper.readTree(category.body()).path("category").asText());

        HttpResponse<String> details = client.send(HttpRequest.newBuilder(api("/api/skills/" + skillId))
                        .header("Cookie", cookie)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                                new DetailPatch("After", "After description", "Full detail", List.of("第一步")))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, details.statusCode());
        JsonNode updated = objectMapper.readTree(details.body());
        assertEquals("After", updated.path("name").asText());
        assertEquals("Full detail", updated.path("detail").asText());
        assertEquals("第一步", updated.path("usageSteps").get(0).asText());
    }

    @Test
    void readsAndUpdatesEnvironmentThroughStaticBrowserRoutes() throws Exception {
        String email = "environment-" + UUID.randomUUID() + "@example.com";
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> register = client.send(HttpRequest.newBuilder(api("/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new Registration(
                                email, "Environment Owner", "StrongPass-2026"))))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, register.statusCode());

        JsonNode registered = objectMapper.readTree(register.body());
        String ownerId = registered.path("user").path("id").asText();
        String cookie = register.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0];
        String skillId = UUID.randomUUID().toString();
        StoredSkillFile stored = fileStorageService.store(
                ownerId, skillId, "sample.zip", new ByteArrayInputStream(skillArchive(Map.of(
                        "sample/SKILL.md", "---\nname: Sample\ndescription: Sample skill\n---\n",
                        "sample/env.properties", "API_URL=https://example.test\nTOKEN=before\n"))));
        skillRepository.save(new SkillEntity(
                skillId, ownerId, "Sample", "Sample skill", "测试技能", "sample.zip",
                stored.path().toString(), "application/zip", stored.sizeBytes(), stored.sha256(), Instant.now()));

        HttpResponse<String> environment = client.send(HttpRequest.newBuilder(
                        api("/api/skills/" + skillId + "/environment"))
                        .header("Cookie", cookie)
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, environment.statusCode());
        JsonNode initial = objectMapper.readTree(environment.body());
        assertTrue(initial.path("exists").asBoolean());
        assertEquals("before", initial.path("values").path("TOKEN").asText());

        HttpResponse<String> update = client.send(HttpRequest.newBuilder(
                        api("/api/skills/" + skillId + "/environment"))
                        .header("Cookie", cookie)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(
                                "{\"values\":{\"TOKEN\":\"after\"}}"))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, update.statusCode());
        assertEquals("after", objectMapper.readTree(update.body()).path("values").path("TOKEN").asText());

        HttpResponse<String> share = client.send(HttpRequest.newBuilder(api("/api/public-skills"))
                        .header("Cookie", cookie)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"skillId\":\"" + skillId + "\"}"))
                        .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(201, share.statusCode());
        String publicSkillId = objectMapper.readTree(share.body()).path("id").asText();

        HttpResponse<String> publicEnvironment = client.send(HttpRequest.newBuilder(
                        api("/api/public-skills/" + publicSkillId + "/environment"))
                        .header("Cookie", cookie)
                        .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, publicEnvironment.statusCode());
        JsonNode publicView = objectMapper.readTree(publicEnvironment.body());
        assertEquals("after", publicView.path("values").path("TOKEN").asText());
        assertEquals(false, publicView.path("editable").asBoolean());
    }

    private static byte[] skillArchive(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    private URI api(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static HttpRequest.Builder authenticated(URI uri, String token) {
        return HttpRequest.newBuilder(uri)
                .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                .header("Authorization", "Bearer " + token);
    }

    private HttpResponse<String> login(HttpClient client, String email, String password) throws Exception {
        return client.send(HttpRequest.newBuilder(api("/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                                new Login(email, password))))
                        .build(), HttpResponse.BodyHandlers.ofString());
    }

    private record Registration(String email, String displayName, String password) {
    }

    private record Login(String email, String password) {
    }

    private record PasswordChange(String currentPassword, String newPassword) {
    }

    private record PasswordReset(String email, String code, String newPassword) {
    }

    private record CategoryPatch(String category) {
    }

    private record DetailPatch(String name, String description, String detail, List<String> usageSteps) {
    }
}

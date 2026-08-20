package com.skillport.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillport.server.domain.UserEntity;
import com.skillport.server.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

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
    private PasswordEncoder passwordEncoder;

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

    private URI api(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static HttpRequest.Builder authenticated(URI uri, String token) {
        return HttpRequest.newBuilder(uri)
                .header("X-SkillPort-Gateway-Key", GATEWAY_KEY)
                .header("Authorization", "Bearer " + token);
    }

    private record Registration(String email, String displayName, String password) {
    }
}

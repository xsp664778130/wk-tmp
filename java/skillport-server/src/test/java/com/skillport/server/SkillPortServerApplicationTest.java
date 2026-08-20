package com.skillport.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SkillPortServerApplicationTest {
    @Test
    void contextLoadsWithMySqlCompatibleSchemaAndNetty() {
    }
}

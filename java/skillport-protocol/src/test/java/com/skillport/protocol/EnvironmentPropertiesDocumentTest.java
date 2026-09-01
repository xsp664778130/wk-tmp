package com.skillport.protocol;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EnvironmentPropertiesDocumentTest {
    @Test
    void readsValuesAndPreservesCommentsAndSpacingWhenUpdating() {
        EnvironmentPropertiesDocument document = EnvironmentPropertiesDocument.parse(
                "# service config\r\nAPI_URL = https://old.example\r\nTOKEN: demo\r\n");

        assertEquals(Map.of("API_URL", "https://old.example", "TOKEN", "demo"), document.values());
        assertEquals("# service config\r\nAPI_URL = https://new.example\r\nTOKEN: demo\r\n",
                document.updateValues(Map.of("API_URL", "https://new.example")));
    }

    @Test
    void rejectsUnknownKeysDuplicateKeysAndMultilineValues() {
        EnvironmentPropertiesDocument document = EnvironmentPropertiesDocument.parse("TOKEN=demo\n");

        assertThrows(IllegalArgumentException.class,
                () -> document.updateValues(Map.of("UNKNOWN", "value")));
        assertThrows(IllegalArgumentException.class,
                () -> EnvironmentPropertiesDocument.parse("TOKEN=a\nTOKEN=b\n"));
        assertThrows(IllegalArgumentException.class,
                () -> document.updateValues(Map.of("TOKEN", "line1\nline2")));
    }
}

package eu.northsoft.bettermob.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {
    @Test
    void versionsAreComparedNumberByNumber() {
        assertTrue(UpdateChecker.isNewer("1.1.9", "1.2.0"));
        assertTrue(UpdateChecker.isNewer("1.1.9", "v1.1.10"));
        assertTrue(UpdateChecker.isNewer("1.1.7", "1.1.7.1"));
        assertFalse(UpdateChecker.isNewer("1.2.0", "1.2.0"));
        assertFalse(UpdateChecker.isNewer("1.2.0", "1.1.9"));
        assertFalse(UpdateChecker.isNewer("1.2.0", "1.2"));
        assertFalse(UpdateChecker.isNewer("1.2.0-SNAPSHOT", "1.2.0"));
        assertTrue(UpdateChecker.isNewer("1.9.0", "2.0.0"));
    }

    @Test
    void theReleaseResponseGivesTheVersionAndTheLink() {
        UpdateChecker.Release release = UpdateChecker.parse("{\"url\":\"x\",\"html_url\":\"https://github.com/HyperGaming99/bettermob/releases/tag/v1.2.0\",\"tag_name\":\"v1.2.0\",\"name\":\"1.2.0\"}");
        assertEquals("1.2.0", release.version());
        assertEquals("https://github.com/HyperGaming99/bettermob/releases/tag/v1.2.0", release.url());
        assertEquals("https://github.com/HyperGaming99/bettermob/releases/latest", UpdateChecker.parse("{\"tag_name\":\"1.3.0\"}").url());
        assertNull(UpdateChecker.parse("{\"message\":\"Not Found\"}"));
    }
}

package eu.northsoft.bettermob.lang;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangFilesTest {
    private static final Path LANG = Path.of("src/main/resources/lang");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\w+)}");
    private static final Pattern KEY_USE = Pattern.compile("messages(?:\\(\\))?\\.(?:get|send|info|warn|severe)\\((?:[^\",()]+,\\s*)?\"([\\w.]+)\"|warnOnce\\(\"([\\w.]+)\"");

    private static TreeMap<String, String> load(String code) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(LANG.resolve(code + ".yml").toFile());
        TreeMap<String, String> messages = new TreeMap<>();
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) messages.put(key, config.getString(key));
        }
        return messages;
    }

    private static Set<String> placeholders(String text) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) found.add(matcher.group(1));
        return found;
    }

    @Test
    void germanHasExactlyTheKeysOfEnglish() {
        assertEquals(load("en").keySet(), load("de").keySet());
    }

    @Test
    void translationsUseTheSamePlaceholders() {
        TreeMap<String, String> english = load("en");
        TreeMap<String, String> german = load("de");
        List<String> mismatches = new ArrayList<>();
        english.forEach((key, text) -> {
            if (!placeholders(text).equals(placeholders(german.get(key)))) mismatches.add(key);
        });
        assertTrue(mismatches.isEmpty(), () -> "placeholders differ for " + mismatches);
    }

    @Test
    void everyKeyUsedInTheCodeExists() throws IOException {
        Set<String> known = load("en").keySet();
        List<String> missing = new ArrayList<>();
        int used = 0;
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = KEY_USE.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (matcher.find()) {
                    String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    used++;
                    if (!known.contains(key)) missing.add(file.getFileName() + ": " + key);
                }
            }
        }
        assertTrue(used > 50, "found only " + used + " message keys in the code");
        assertTrue(missing.isEmpty(), () -> "keys missing from lang/en.yml: " + missing);
    }

    @Test
    void everyKeyInEnglishIsUsedInTheCode() throws IOException {
        StringBuilder source = new StringBuilder();
        try (Stream<Path> walk = Files.walk(Path.of("src/main/java"))) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                source.append(Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        List<String> unused = new ArrayList<>();
        for (String key : load("en").keySet()) {
            if (!source.toString().contains("\"" + key + "\"")) unused.add(key);
        }
        assertTrue(unused.isEmpty(), () -> "keys never used in the code: " + unused);
    }
}

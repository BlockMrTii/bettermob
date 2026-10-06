package eu.northsoft.bettermob.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemasTest {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path SCHEMAS = Path.of("schemas");
    private static final List<Path> ROOTS = List.of(Path.of("src/main/resources"), Path.of("src/test/resources/packs"));

    private static JsonSchema schema(String name) throws IOException {
        JsonNode node = JSON.readTree(SCHEMAS.resolve(name + ".schema.json").toFile());
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(node);
    }

    private static String kindOf(Path relative) {
        for (int i = relative.getNameCount() - 2; i >= 0; i--) {
            String name = relative.getName(i).toString().toLowerCase(Locale.ROOT);
            if (Set.of("mobs", "skills", "items").contains(name)) return name.substring(0, name.length() - 1);
        }
        return null;
    }

    @Test
    void everyShippedAndSampleFileMatchesItsSchema() throws IOException {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        for (Path root : ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".yml")).toList()) {
                    String kind = kindOf(root.relativize(file));
                    if (kind == null) continue;
                    checked++;
                    for (ValidationMessage message : schema(kind).validate(YAML.readTree(file.toFile()))) {
                        failures.add(file + ": " + message.getMessage());
                    }
                }
            }
        }
        assertTrue(checked > 0, "no files found");
        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void theSchemasRejectWrongTypesAndMissingFields() throws IOException {
        assertFalse(schema("mob").validate(YAML.readTree("Type: PIG\nHealth: lots\n")).isEmpty());
        assertFalse(schema("mob").validate(YAML.readTree("Type: PIG\nSkills:\n  - sound{s=x} @self\n")).isEmpty());
        assertFalse(schema("mob").validate(YAML.readTree("Type: PIG\nModules:\n  BossBar:\n    Color: ORANGE\n")).isEmpty());
        assertFalse(schema("item").validate(YAML.readTree("my_item:\n  Display: x\n")).isEmpty());
        assertFalse(schema("skill").validate(YAML.readTree("s:\n  Cooldown: -1\n")).isEmpty());
    }

    @Test
    void theSchemasAcceptBothMobFileLayouts() throws IOException {
        assertTrue(schema("mob").validate(YAML.readTree("Type: zombie\nHealth: 30\nSpawn:\n  Time: night\n")).isEmpty());
        assertTrue(schema("mob").validate(YAML.readTree("a:\n  Type: PIG\nb:\n  Type: COW\n  Skills:\n    - sound{s=x} @self ~onSpawn\n")).isEmpty());
    }
}

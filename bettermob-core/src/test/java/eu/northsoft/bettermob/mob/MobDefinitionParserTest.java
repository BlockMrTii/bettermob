package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.YamlConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobDefinitionParserTest {

    private static final String SKELETON_KNIGHT_YAML = """
            Type: SKELETON
            Display: '&c&lSkeleton Knight'
            Model: skeleton_knight
            Health: 60
            Damage: 8
            RemoveAi: false
            AIGoalSelectors:
              - clear
              - randomstroll
            AITargetSelectors:
              - clear
            Options:
              Collidable: true
              MovementSpeed: 0.25
              PreventOtherDrops: true
              Silent: true
            Modules:
              ThreatTable: true
            DamageModifiers:
              - FIRE 1.2
            """;

    @Test
    void parsesFlatMobDefinition() {
        ConfigSection config = YamlConfig.load(SKELETON_KNIGHT_YAML);
        Map<String, ConfigSection> sections = MobDefinitionParser.extractSections("skeleton_knight", config);
        assertEquals(1, sections.size());
        assertTrue(sections.containsKey("skeleton_knight"));

        List<String> warnings = new ArrayList<>();
        MobDefinition def = MobDefinitionParser.parse("skeleton_knight", sections.get("skeleton_knight"), warnings::add);

        assertNotNull(def);
        assertTrue(warnings.isEmpty(), warnings.toString());
        assertEquals("skeleton_knight", def.id);
        assertEquals("SKELETON", def.type);
        assertEquals("&c&lSkeleton Knight", def.displayName);
        assertEquals("skeleton_knight", def.modelId);
        assertEquals(60.0, def.health);
        assertEquals(8.0, def.damage);
        assertFalse(def.removeAi);
        assertEquals(List.of("clear", "randomstroll"), def.aiGoalSelectors);
        assertEquals(List.of("clear"), def.aiTargetSelectors);
        assertTrue(def.options.collidable());
        assertEquals(0.25, def.options.movementSpeed());
        assertTrue(def.options.silent());
        assertTrue(def.threatTable);
        assertEquals(1.2, def.damageModifiers.get("FIRE"));
    }

    @Test
    void parsesMultiMobFile() {
        String yaml = """
                mob1:
                  Type: ZOMBIE
                  Health: 40
                mob2:
                  Type: SKELETON
                  Health: 30
                """;
        ConfigSection config = YamlConfig.load(yaml);
        Map<String, ConfigSection> sections = MobDefinitionParser.extractSections("mobs", config);
        assertEquals(2, sections.size());

        MobDefinition m1 = MobDefinitionParser.parse("mob1", sections.get("mob1"), null);
        MobDefinition m2 = MobDefinitionParser.parse("mob2", sections.get("mob2"), null);

        assertNotNull(m1);
        assertEquals("ZOMBIE", m1.type);
        assertEquals(40.0, m1.health);

        assertNotNull(m2);
        assertEquals("SKELETON", m2.type);
        assertEquals(30.0, m2.health);
    }
}

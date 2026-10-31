package eu.northsoft.bettermob.mob;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobPhaseTest {
    private static List<MobPhase> parse(String yaml, List<String> invalid) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return MobPhase.parse(new eu.northsoft.bettermob.paper.PaperConfigSection(config), invalid::add);
    }

    private static final String TWO_PHASES = """
            Phases:
              - Health: '<70%'
                Display: '&cAngry'
                OnStart:
                  - sound{s=entity.zombie.ambient} @self
                Skills:
                  - sound{s=entity.zombie.hurt} @self ~onDamaged
              - Health: '<=30%'
                Model: enraged
                Replace: true
                Skills:
                  - sound{s=entity.zombie.death} @self ~onDamaged
            """;

    @Test
    void phasesAreParsedWithTheirOptions() throws Exception {
        List<String> invalid = new ArrayList<>();
        List<MobPhase> phases = parse(TWO_PHASES, invalid);
        assertTrue(invalid.isEmpty(), invalid.toString());
        assertEquals(2, phases.size());
        assertEquals("&cAngry", phases.get(0).display());
        assertEquals(1, phases.get(0).start().size());
        assertEquals(1, phases.get(0).triggersOf(MobDefinition.SkillTrigger.Trigger.DAMAGED).size());
        assertEquals("enraged", phases.get(1).model());
        assertEquals(true, phases.get(1).definition().get("Replace"));
        assertTrue(phases.get(0).triggersOf(MobDefinition.SkillTrigger.Trigger.DEATH).isEmpty());
    }

    @Test
    void optionOverridesAndEquipmentAreStored() throws Exception {
        List<String> invalid = new ArrayList<>();
        List<MobPhase> phases = parse("""
                Options:
                  Silent: true
                  MovementSpeed: 0.25
                Phases:
                  - Health: '<70%'
                    Options:
                      MovementSpeed: 0.5
                      CanMove: false
                      PreventOtherDrops: true
                    Equipment: [IRON_SWORD:HAND]
                    Skills:
                      - sound{s=start} @self ~onPhase
                  - Health: '<30%'
                    Equipment: []
                """, invalid);
        assertTrue(invalid.isEmpty(), invalid.toString());
        assertEquals(0.5, phases.get(0).options().movementSpeed());
        assertTrue(phases.get(0).options().silent());
        assertTrue(phases.get(0).options().preventOtherDrops());
        assertEquals(false, phases.get(0).options().canMove());
        assertEquals(List.of("IRON_SWORD:HAND"), phases.get(0).equipment());
        assertEquals(1, phases.get(0).triggersOf(MobDefinition.SkillTrigger.Trigger.PHASE).size());
        assertEquals(List.of(), phases.get(1).equipment());
        assertEquals(0.25, phases.get(1).options().movementSpeed());
    }

    @Test
    void malformedFieldsAreReported() throws Exception {
        List<String> invalid = new ArrayList<>();
        parse("""
                Phases:
                  - not a map
                  - Health: '<50%'
                    Options:
                      Silent: loud
                      Scale: -1
                      Unknown: true
                    Equipment: [IRON_SWORD:UNKNOWN]
                    Skills: bad
                    Model: 12
                    Display:
                    Other: true
                """, invalid);
        assertTrue(invalid.size() >= 8, invalid.toString());
        assertTrue(invalid.stream().allMatch(line -> line.startsWith("Phases[")));
    }

    @Test
    void theLastMatchingPhaseWinsAndNoneMatchesAtFullHealth() throws Exception {
        List<MobPhase> phases = parse(TWO_PHASES, new ArrayList<>());
        assertEquals(-1, MobPhase.select(phases, 100, 100));
        assertEquals(0, MobPhase.select(phases, 60, 100));
        assertEquals(1, MobPhase.select(phases, 30, 100));
        assertEquals(0, MobPhase.select(phases, 50, 100));
        assertEquals(-1, MobPhase.select(List.of(), 10, 100));
    }

    @Test
    void invalidPhasesAndLinesAreReportedAndSkipped() throws Exception {
        List<String> invalid = new ArrayList<>();
        List<MobPhase> phases = parse("""
                Phases:
                  - Health: 'half'
                  - Health: '<50%'
                    OnStart:
                      - ''
                    Skills:
                      - nonsense
                """, invalid);
        assertEquals(1, phases.size());
        assertEquals(3, invalid.size(), invalid.toString());
        assertNotNull(MobPhase.parseHealth(" <= 30 % "));
        assertNull(MobPhase.parseHealth("30%"));
    }
}

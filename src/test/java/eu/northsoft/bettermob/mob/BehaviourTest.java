package eu.northsoft.bettermob.mob;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviourTest {
    private static Behaviour parse(String yaml, List<String> invalid) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return Behaviour.parse(config, invalid::add);
    }

    @Test
    void waypointsAcceptThreeOrFourNumbers() {
        assertEquals(new Behaviour.Waypoint(null, 1, 64, -3.5), Behaviour.parsePoint(" 1 64 -3.5 "));
        assertEquals(new Behaviour.Waypoint("world_nether", 0, 70, 12), Behaviour.parsePoint("World_Nether 0 70 12"));
        assertNull(Behaviour.parsePoint("1 2"));
        assertNull(Behaviour.parsePoint("a b c"));
        assertNull(Behaviour.parsePoint("1 NaN 3"));
        assertNull(Behaviour.parsePoint("w 1 2 3 4"));
    }

    @Test
    void aPatrolListUsesLoopingWithoutWaiting() throws Exception {
        Behaviour behaviour = parse("Patrol:\n  - 1 64 1\n  - 5 64 1\n", new ArrayList<>());
        assertEquals(2, behaviour.patrol().size());
        assertTrue(behaviour.loop());
        assertEquals(0, behaviour.waitTicks());
        assertFalse(behaviour.hasGuard());
        assertFalse(behaviour.needsHome());
    }

    @Test
    void aPatrolSectionTakesLoopAndWaitInSeconds() throws Exception {
        Behaviour behaviour = parse("Patrol:\n  Points:\n    - 1 64 1\n  Loop: false\n  Wait: 2.5\n", new ArrayList<>());
        assertFalse(behaviour.loop());
        assertEquals(50, behaviour.waitTicks());
    }

    @Test
    void guardAndHomeDistanceNeedAHomePoint() throws Exception {
        Behaviour sectionForm = parse("Guard:\n  Radius: 12\nMaxHomeDistance: 40\n", new ArrayList<>());
        assertEquals(12, sectionForm.guardRadius());
        assertEquals(40, sectionForm.maxHomeDistance());
        assertTrue(sectionForm.hasGuard() && sectionForm.hasHomeLimit() && sectionForm.needsHome());
        assertEquals(8, parse("Guard: 8\n", new ArrayList<>()).guardRadius());
    }

    @Test
    void invalidPointsAreReportedAndNothingMeansNoBehaviour() throws Exception {
        List<String> invalid = new ArrayList<>();
        Behaviour behaviour = parse("Patrol:\n  - 1 64 1\n  - nonsense\n", invalid);
        assertEquals(1, behaviour.patrol().size());
        assertEquals(List.of("Patrol: nonsense"), invalid);
        assertNull(parse("Type: PIG\n", new ArrayList<>()));
        assertNull(parse("Guard: 0\nMaxHomeDistance: -4\n", new ArrayList<>()));
    }
}

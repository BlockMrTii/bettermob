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

    @Test
    void aQuotedFalseStopsTheLoopAndAWrongShapeIsReported() throws Exception {
        assertFalse(parse("Patrol:\n  Points:\n    - 1 64 1\n  Loop: \"false\"\n", new ArrayList<>()).loop());
        List<String> invalid = new ArrayList<>();
        parse("Patrol:\n  Points: 1 64 1\nGuard: 5\n", invalid);
        assertEquals(List.of("Patrol: Points 1 64 1"), invalid);
    }

    @Test
    void groupAndLeaderAreParsedWithDefaultsAndLimits() throws Exception {
        Behaviour group = parse("Group: Orcs\n", new ArrayList<>());
        assertEquals("orcs", group.group());
        assertEquals(Behaviour.DEFAULT_ALERT_RADIUS, group.alertRadius());
        assertTrue(group.hasGroup());
        assertEquals(128, parse("Group: a\nAlertRadius: 9000\n", new ArrayList<>()).alertRadius());
        assertFalse(parse("Group: a\nAlertRadius: 0\n", new ArrayList<>()).hasGroup());

        Behaviour text = parse("Leader: Orc_Chief\n", new ArrayList<>());
        assertEquals(new Behaviour.Leader("orc_chief", 4, 32, Behaviour.LeaderLoss.FIND), text.leader());
        Behaviour section = parse("Leader:\n  Mob: chief\n  Distance: 6\n  Range: 50\n  OnLeaderDeath: home\n", new ArrayList<>());
        assertEquals(new Behaviour.Leader("chief", 6, 50, Behaviour.LeaderLoss.HOME), section.leader());
        assertTrue(section.needsHome());
    }

    @Test
    void invalidLeaderSettingsAreReported() throws Exception {
        List<String> invalid = new ArrayList<>();
        Behaviour behaviour = parse("Leader:\n  Mob: chief\n  OnLeaderDeath: explode\n", invalid);
        assertEquals(Behaviour.LeaderLoss.FIND, behaviour.leader().onLoss());
        assertEquals(List.of("Leader: OnLeaderDeath explode"), invalid);
        List<String> missing = new ArrayList<>();
        assertNull(parse("Leader:\n  Distance: 3\n", missing));
        assertEquals(1, missing.size());
    }
}

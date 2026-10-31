package eu.northsoft.bettermob.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabCompletionTest {
    private static final List<String> MOBS = List.of("nm_wumpus", "otter", "NM_beaver", "zombie_brute", "nm_otter");

    @Test
    void onlyOptionsStartingWithTheTypedTextRemainIgnoringCase() {
        assertEquals(List.of("NM_beaver", "nm_otter", "nm_wumpus"), BetterMobCommand.startingWith(MOBS, "nm_"));
        assertEquals(List.of("otter"), BetterMobCommand.startingWith(MOBS, "ott"));
        assertEquals(List.of(), BetterMobCommand.startingWith(MOBS, "xyz"));
    }

    @Test
    void anEmptyPrefixKeepsEverythingSortedWithoutDuplicates() {
        assertEquals(List.of("NM_beaver", "nm_otter", "nm_wumpus", "otter", "zombie_brute"), BetterMobCommand.startingWith(MOBS, ""));
        assertEquals(List.of("a", "b"), BetterMobCommand.startingWith(List.of("b", "a", "b"), ""));
    }
}

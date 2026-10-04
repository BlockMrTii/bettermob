package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillStateTest {
    @Test
    void acquireRespectsCooldown() {
        SkillState state = new SkillState();
        assertTrue(state.acquire("a", 60_000));
        assertFalse(state.acquire("a", 60_000));
    }

    @Test
    void expiredCooldownsArePurgedOnceTheMapGrowsLarge() {
        SkillState state = new SkillState();
        for (int i = 0; i < 3000; i++) state.acquire("expired" + i, 0);
        state.acquire("live", 60_000);
        assertTrue(state.cooldownCount() < 3000);
        assertFalse(state.acquire("live", 60_000));
    }
}

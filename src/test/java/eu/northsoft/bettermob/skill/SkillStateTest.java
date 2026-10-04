package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void forgetDropsAurasAndGcdOfTheEntity() {
        SkillState state = new SkillState();
        UUID gone = UUID.randomUUID();
        UUID kept = UUID.randomUUID();
        state.aurasOf(gone);
        state.aurasOf(kept);
        state.setGcd(gone, 1200);
        state.setGcd(kept, 1200);
        state.forget(gone);
        assertNull(state.activeAuras(gone));
        assertNotNull(state.activeAuras(kept));
        assertFalse(state.hasActiveGcd(gone));
        assertTrue(state.hasActiveGcd(kept));
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

package eu.northsoft.bettermob.api;

import eu.northsoft.bettermob.api.event.BetterMobDamageEvent;
import eu.northsoft.bettermob.api.event.BetterMobSkillEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiEventsTest {
    @Test
    void theDamageEventCarriesItsValuesAndNeverGoesNegative() {
        BetterMobDamageEvent event = new BetterMobDamageEvent(null, null, null, true, DamageCause.FIRE, 4.5);
        assertTrue(event.isMobVictim());
        assertEquals(DamageCause.FIRE, event.getCause());
        assertEquals(4.5, event.getDamage());
        event.setDamage(-3);
        assertEquals(0.0, event.getDamage());
        event.setDamage(7);
        assertEquals(7.0, event.getDamage());
    }

    @Test
    void bothEventsCanBeCancelled() {
        BetterMobDamageEvent damage = new BetterMobDamageEvent(null, null, null, false, DamageCause.ENTITY_ATTACK, 1);
        BetterMobSkillEvent skill = new BetterMobSkillEvent("my_skill", null, null, null);
        assertFalse(damage.isCancelled());
        assertFalse(skill.isCancelled());
        damage.setCancelled(true);
        skill.setCancelled(true);
        assertTrue(damage.isCancelled());
        assertTrue(skill.isCancelled());
    }

    @Test
    void theSkillEventExposesTheSkillId() {
        BetterMobSkillEvent skill = new BetterMobSkillEvent("my_skill", null, null, null);
        assertEquals("my_skill", skill.getSkillId());
        assertNull(skill.getMob());
        assertNotNull(BetterMobSkillEvent.getHandlerList());
        assertNotNull(BetterMobDamageEvent.getHandlerList());
    }
}

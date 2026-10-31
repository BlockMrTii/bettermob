package eu.northsoft.bettermob.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossBarTitleTest {
    @Test
    void replacesTheMobPlaceholders() {
        assertEquals("§cBison §7(12/20) bison_red",
                BossBarManager.renderTitle("&c<mob.name> &7(<mob.hp>/<mob.maxhp>) <mob.id>", "bison_red", "Bison", 12, 20));
    }

    @Test
    void healthIsRoundedUpSoAnAliveMobNeverShowsZero() {
        assertEquals("1/20", BossBarManager.renderTitle("<mob.hp>/<mob.maxhp>", "m", "M", 0.2, 20));
        assertEquals("13/21", BossBarManager.renderTitle("<mob.hp>/<mob.maxhp>", "m", "M", 12.4, 20.5));
    }

    @Test
    void theDefaultTitleIsTheDisplayName() {
        assertEquals("§9Wumpus", BossBarManager.renderTitle(BossBarSettings.DEFAULT_TITLE, "nm_wumpus", "&9Wumpus", 5, 5));
    }

    @Test
    void textWithoutPlaceholdersIsKept() {
        assertEquals("Boss fight", BossBarManager.renderTitle("Boss fight", "m", "M", 1, 1));
    }
}

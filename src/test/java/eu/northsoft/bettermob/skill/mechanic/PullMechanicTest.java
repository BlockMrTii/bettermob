package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PullMechanicTest {
    @Test
    void pullGrowsWithDistanceAndIsCapped() {
        assertEquals(new Vector(0.7, 0, 0), PullMechanic.pullVelocity(new Vector(0, 0, 0), new Vector(1, 0, 0), 7));
        assertEquals(4.0, PullMechanic.pullVelocity(new Vector(0, 0, 0), new Vector(100, 0, 0), 10).length(), 1e-9);
    }

    @Test
    void propelGoesTowardsTheTargetOrAlongTheFacing() {
        assertEquals(new Vector(0, 0, 1), PullMechanic.propelVelocity(new Vector(0, 0, 0), new Vector(0, 0, 5), new Vector(1, 0, 0), 10));
        assertEquals(new Vector(2, 0, 0), PullMechanic.propelVelocity(new Vector(3, 0, 3), new Vector(3, 0, 3), new Vector(1, 0, 0), 20));
        assertEquals(4.0, PullMechanic.propelVelocity(new Vector(0, 0, 0), new Vector(0, 0, 1), new Vector(), 500).length(), 1e-9);
    }
}

package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileMotionTest {
    @Test
    void reflectingOffAFloorFlipsOnlyTheVerticalPart() {
        Vector out = ProjectileMotion.reflect(new Vector(1, -1, 0), new Vector(0, 1, 0));
        assertEquals(Math.sqrt(0.5), out.getX(), 1.0e-9);
        assertEquals(Math.sqrt(0.5), out.getY(), 1.0e-9);
        assertEquals(1, out.length(), 1.0e-9);
    }

    @Test
    void steeringTurnsAtMostTheAllowedAngleAndReachesCloseTargets() {
        Vector from = new Vector(1, 0, 0);
        Vector turned = ProjectileMotion.steer(from, new Vector(0, 0, 1), Math.toRadians(10));
        assertEquals(10, Math.toDegrees(Math.acos(from.dot(turned))), 0.5);
        assertEquals(1, turned.length(), 1.0e-9);
        Vector close = ProjectileMotion.steer(from, new Vector(1, 0.01, 0), Math.toRadians(10));
        assertEquals(new Vector(1, 0.01, 0).normalize().getY(), close.getY(), 1.0e-9);
        assertTrue(ProjectileMotion.steer(from, new Vector(0, 0, 0), 1).equals(from));
    }

    @Test
    void steeringAgainstTheDirectionStillTurns() {
        Vector turned = ProjectileMotion.steer(new Vector(1, 0, 0), new Vector(-1, 0, 0), Math.toRadians(10));
        assertEquals(1, turned.length(), 1.0e-9);
    }
}

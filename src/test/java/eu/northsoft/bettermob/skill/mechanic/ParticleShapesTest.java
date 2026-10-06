package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleShapesTest {
    private static final double EPS = 1e-9;

    @Test
    void aLineStartsAndEndsAtItsEndpointsWithEvenSteps() {
        List<Vector> line = ParticleShapes.line(new Vector(0, 0, 0), new Vector(10, 0, 0), 11);
        assertEquals(11, line.size());
        assertEquals(0, line.get(0).getX(), EPS);
        assertEquals(10, line.get(10).getX(), EPS);
        assertEquals(5, line.get(5).getX(), EPS);
    }

    @Test
    void aLineHasAtLeastTwoPointsAndTheCountIsCapped() {
        assertEquals(2, ParticleShapes.line(new Vector(), new Vector(1, 0, 0), 1).size());
        assertEquals(ParticleShapes.MAX_POINTS, ParticleShapes.line(new Vector(), new Vector(1, 0, 0), 100000).size());
        assertEquals(ParticleShapes.MAX_POINTS, ParticleShapes.clamp(9999));
        assertEquals(1, ParticleShapes.clamp(-4));
    }

    @Test
    void theLinePointCountFollowsTheDensity() {
        assertEquals(21, ParticleShapes.linePoints(new Vector(0, 0, 0), new Vector(10, 0, 0), 2));
        assertTrue(ParticleShapes.linePoints(new Vector(0, 0, 0), new Vector(1000, 0, 0), 2) <= ParticleShapes.MAX_POINTS);
    }

    @Test
    void everySpherePointLiesOnTheSphere() {
        List<Vector> sphere = ParticleShapes.sphere(2.5, 50);
        assertEquals(50, sphere.size());
        for (Vector point : sphere) assertEquals(2.5, point.length(), 1e-9);
        assertEquals(2.5, sphere.get(0).getY(), EPS);
        assertEquals(-2.5, sphere.get(49).getY(), EPS);
    }

    @Test
    void aHelixRisesFromTheBottomToItsHeightOnItsRadius() {
        List<Vector> helix = ParticleShapes.helix(1.5, 4, 2, 41);
        assertEquals(41, helix.size());
        assertEquals(0, helix.get(0).getY(), EPS);
        assertEquals(4, helix.get(40).getY(), EPS);
        for (Vector point : helix) assertEquals(1.5, Math.hypot(point.getX(), point.getZ()), 1e-9);
        assertTrue(helix.get(20).getY() > helix.get(10).getY());
    }
}

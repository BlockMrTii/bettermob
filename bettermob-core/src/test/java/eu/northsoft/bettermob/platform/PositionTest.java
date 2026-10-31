package eu.northsoft.bettermob.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionTest {
    @Test
    void distanceAcrossWorldsIsInfinite() {
        assertEquals(Double.POSITIVE_INFINITY, new Position("a", 0, 0, 0).distanceSquared(new Position("b", 0, 0, 0)));
        assertEquals(25, new Position("a", 0, 0, 0).distanceSquared(new Position("a", 3, 4, 0)));
    }

    @Test
    void blockCoordinatesFloor() {
        Position position = new Position("w", -0.5, 64.9, 1.2);
        assertEquals(-1, position.blockX());
        assertEquals(64, position.blockY());
        assertEquals(1, position.blockZ());
    }

    @Test
    void addKeepsRotation() {
        Position moved = new Position("w", 1, 2, 3, 90f, 10f).add(1, 1, 1);
        assertEquals(new Position("w", 2, 3, 4, 90f, 10f), moved);
    }

    @Test
    void worldIsRequired() {
        assertThrows(NullPointerException.class, () -> new Position(null, 0, 0, 0));
    }

    @Test
    void vectorMath() {
        assertEquals(5, new Vec3(3, 4, 0).length());
        assertEquals(Vec3.ZERO, Vec3.ZERO.normalize());
        assertEquals(new Vec3(1, 0, 0), new Vec3(5, 0, 0).normalize());
    }
}

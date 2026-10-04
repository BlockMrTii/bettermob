package eu.northsoft.bettermob.util;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionEntitiesTest {
    @Test
    void chunkOfRoundsTowardsNegativeInfinity() {
        assertEquals(0, RegionEntities.chunkOf(15.9));
        assertEquals(1, RegionEntities.chunkOf(16));
        assertEquals(-1, RegionEntities.chunkOf(-0.5));
        assertEquals(-2, RegionEntities.chunkOf(-16.5));
    }

    @Test
    void invalidRadiiFindNothing() {
        Location center = new Location(null, 0, 64, 0);
        assertTrue(RegionEntities.near(center, Double.NaN).isEmpty());
        assertTrue(RegionEntities.near(center, Double.POSITIVE_INFINITY).isEmpty());
        assertTrue(RegionEntities.near(center, -1).isEmpty());
    }
}

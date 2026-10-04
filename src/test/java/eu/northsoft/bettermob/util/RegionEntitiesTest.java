package eu.northsoft.bettermob.util;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void boxCheckUsesEveryAxis() {
        Location center = new Location(null, 0, 64, 0);
        assertTrue(RegionEntities.withinBox(new Location(null, 5, 64, -5), center, 5));
        assertFalse(RegionEntities.withinBox(new Location(null, 5, 70, 0), center, 5));
    }
}

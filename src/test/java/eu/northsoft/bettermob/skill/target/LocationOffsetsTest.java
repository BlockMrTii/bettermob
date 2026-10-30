package eu.northsoft.bettermob.skill.target;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationOffsetsTest {
    @Test
    void offsetsFollowTheYaw() {
        Location south = new Location(null, 0, 64, 0, 0, 0);
        Location moved = LocationOffsets.apply(south, Map.of("fo", "6", "yo", "0.25", "so", "1"));
        assertEquals(-1.0, moved.getX(), 1e-9);
        assertEquals(64.25, moved.getY(), 1e-9);
        assertEquals(6.0, moved.getZ(), 1e-9);
        Location west = new Location(null, 0, 64, 0, 90, 0);
        assertEquals(-2.0, LocationOffsets.apply(west, Map.of("forwardoffset", "2")).getX(), 1e-9);
    }
}

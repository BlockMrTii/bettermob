package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaperAdaptersTest {
    @Test
    void locationRoundTrip() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("world_nether");
        Location location = new Location(world, 1.5, 64, -3.25, 45f, -10f);

        Position position = PaperAdapters.position(location);
        assertEquals(new Position("world_nether", 1.5, 64, -3.25, 45f, -10f), position);
        assertEquals(location, PaperAdapters.location(world, position));
    }

    @Test
    void vectorRoundTrip() {
        assertEquals(new Vec3(1, 2, 3), PaperAdapters.vec(new Vector(1, 2, 3)));
        assertEquals(new Vector(1, 2, 3), PaperAdapters.vector(new Vec3(1, 2, 3)));
    }

    @Test
    void livingEntitiesGetLivingWrapper() {
        assertInstanceOf(PlatformLivingEntity.class, PaperAdapters.entity(mock(LivingEntity.class)));
        assertFalse(PaperAdapters.entity(mock(Entity.class)) instanceof PlatformLivingEntity);
        assertNull(PaperAdapters.entity(null));
    }

    @Test
    void unwrapsToTheSameBukkitEntity() {
        Entity entity = mock(Entity.class);
        assertSame(entity, PaperAdapters.bukkit(PaperAdapters.entity(entity)));
    }
}

package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.FireworkEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldEffectsTest {
    @Test
    void displayValuesAreClampedVectorsAndCombine() {
        assertEquals(new Vector3f(2, 2, 2), DisplayTransformMechanic.vector("2,2,2"));
        assertEquals(new Vector3f(64, -1, 0.5f), DisplayTransformMechanic.vector("100,-1,0.25*2"));
        assertNull(DisplayTransformMechanic.vector("1,2"));
        assertEquals(new Vector3f(3, 3, 3), DisplayTransformMechanic.combine(new Vector3f(1, 1, 1), new Vector3f(2, 2, 2), "ADD"));
        assertEquals(new Vector3f(2, 4, 6), DisplayTransformMechanic.combine(new Vector3f(1, 2, 3), new Vector3f(2, 2, 2), "MULTIPLY"));
        assertEquals(new Vector3f(5, 5, 5), DisplayTransformMechanic.combine(new Vector3f(1, 2, 3), new Vector3f(5, 5, 5), "SET"));
    }

    @Test
    void theRayHitsBoxesNearTheLineOnly() {
        BoundingBox kart = new BoundingBox(-0.5, 0, -0.5, 0.5, 1, 0.5);
        assertTrue(RayTraceToMechanic.crosses(kart, new Vector(-5, 0.5, 0), new Vector(5, 0.5, 0), 0));
        assertFalse(RayTraceToMechanic.crosses(kart, new Vector(-5, 0.5, 2), new Vector(5, 0.5, 2), 0));
        assertTrue(RayTraceToMechanic.crosses(kart, new Vector(-5, 0.5, 1.2), new Vector(5, 0.5, 1.2), 2));
        assertFalse(RayTraceToMechanic.crosses(kart, new Vector(2, 0.5, 0), new Vector(5, 0.5, 0), 0));
    }

    @Test
    void blockMasksAreCubesOrSpheres() {
        assertTrue(BlockMaskMechanic.inShape(1, 1, 1, 1, false));
        assertFalse(BlockMaskMechanic.inShape(1, 1, 1, 1, true));
        assertTrue(BlockMaskMechanic.inShape(0, 1, 0, 1, true));
    }

    @Test
    void fireworkTypeAndColorsFallBack() {
        assertEquals(FireworkEffect.Type.STAR, FireworkMechanic.type("STAR"));
        assertEquals(FireworkEffect.Type.BALL, FireworkMechanic.type("nonsense"));
    }
}

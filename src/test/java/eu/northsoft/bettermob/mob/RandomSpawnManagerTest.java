package eu.northsoft.bettermob.mob;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RandomSpawnManagerTest {
    private enum Kind { SOLID, AIR, LAVA }

    private static World nether(IntFunction<Kind> column) {
        World world = mock(World.class);
        when(world.getMinHeight()).thenReturn(0);
        when(world.getLogicalHeight()).thenReturn(128);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenAnswer(call -> block(column, call.getArgument(1)));
        return world;
    }

    private static Block block(IntFunction<Kind> column, int y) {
        Kind kind = column.apply(y);
        Block block = mock(Block.class);
        when(block.isSolid()).thenReturn(kind == Kind.SOLID);
        when(block.isPassable()).thenReturn(kind != Kind.SOLID);
        when(block.isLiquid()).thenReturn(kind == Kind.LAVA);
        when(block.getRelative(0, 1, 0)).thenAnswer(call -> block(column, y + 1));
        when(block.getRelative(0, 2, 0)).thenAnswer(call -> block(column, y + 2));
        return block;
    }

    private static IntFunction<Kind> floorAt(int floor) {
        return y -> y >= 123 || y <= floor ? Kind.SOLID : Kind.AIR;
    }

    @Test
    void findsTheFloorBelowTheBedrockRoof() {
        assertEquals(64, RandomSpawnManager.groundBelowCeiling(nether(floorAt(64)), 0, 70, 0));
    }

    @Test
    void neverPicksTheRoof() {
        assertEquals(110, RandomSpawnManager.groundBelowCeiling(nether(floorAt(110)), 0, 120, 0));
    }

    @Test
    void skipsLava() {
        World world = nether(y -> y >= 123 || y < 60 ? Kind.SOLID : y <= 65 ? Kind.LAVA : Kind.AIR);
        assertEquals(Integer.MIN_VALUE, RandomSpawnManager.groundBelowCeiling(world, 0, 66, 0));
    }

    @Test
    void findsAFloorFarBelowThePlayer() {
        assertEquals(20, RandomSpawnManager.groundBelowCeiling(nether(floorAt(20)), 0, 70, 0));
    }

    @Test
    void ignoresFloorsHighAboveThePlayer() {
        World world = nether(y -> y >= 123 || y == 100 || y <= 30 ? Kind.SOLID : Kind.AIR);
        assertEquals(30, RandomSpawnManager.groundBelowCeiling(world, 0, 70, 0));
    }

    @Test
    void standsOnSolidGroundWithTwoFreeBlocks() {
        assertTrue(RandomSpawnManager.standable(block(floorAt(64), 64)));
        assertFalse(RandomSpawnManager.standable(block(floorAt(64), 65)));
        assertFalse(RandomSpawnManager.standable(block(floorAt(64), 122)));
    }
}

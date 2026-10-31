package eu.northsoft.bettermob.item;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EggItemsTest {
    @Test
    void theEggMaterialFollowsTheEntityType() {
        assertEquals(Material.PIG_SPAWN_EGG, EggItems.materialFor(EntityType.PIG));
        assertEquals(Material.SKELETON_SPAWN_EGG, EggItems.materialFor(EntityType.SKELETON));
        assertEquals(Material.ZOMBIE_SPAWN_EGG, EggItems.materialFor(EntityType.ZOMBIE));
    }

    @Test
    void typesWithoutAnEggFallBackToTheZombieEgg() {
        assertEquals(EggItems.FALLBACK, EggItems.materialFor(EntityType.ARMOR_STAND));
        assertEquals(EggItems.FALLBACK, EggItems.materialFor(EntityType.ITEM_DISPLAY));
    }
}

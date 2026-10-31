package eu.northsoft.bettermob.item;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public final class EquipmentSupport {
    private EquipmentSupport() {}

    public static ItemStack itemFor(ItemRegistry items, String name) {
        ItemDefinition custom = items.get(name);
        if (custom != null) return items.create(custom, 1);
        Material material = Material.matchMaterial(name);
        return material == null || !material.isItem() ? null : new ItemStack(material);
    }

    public static EquipmentSlot slotFor(String name) {
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "offhand", "off_hand" -> EquipmentSlot.OFF_HAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HAND;
        };
    }

    public static boolean isKnownSlot(String name) {
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "hand", "mainhand", "main_hand", "offhand", "off_hand", "head", "helmet", "chest", "chestplate",
                    "legs", "leggings", "feet", "boots" -> true;
            default -> false;
        };
    }

    public static void equip(LivingEntity living, EquipmentSlot slot, ItemStack stack) {
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return;
        equipment.setItem(slot, stack);
        if (living instanceof Mob) equipment.setDropChance(slot, 0f);
    }
}

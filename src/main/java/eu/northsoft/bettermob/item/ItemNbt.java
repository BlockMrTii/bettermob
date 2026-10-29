package eu.northsoft.bettermob.item;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ItemNbt {
    private static final Pattern INVALID = Pattern.compile("[^a-z0-9_.-]");

    private ItemNbt() {}

    public static NamespacedKey key(String name) {
        String clean = name == null ? "" : INVALID.matcher(name.trim().toLowerCase(Locale.ROOT)).replaceAll("_");
        return clean.isEmpty() ? null : new NamespacedKey("bettermob", "nbt_" + clean);
    }

    public static ItemStack heldItem(Entity entity) {
        return heldItem(entity, null);
    }

    public static ItemStack heldItem(Entity entity, Cancellable event) {
        if (entity instanceof Player player && event instanceof PlayerItemHeldEvent held && player.equals(held.getPlayer())) {
            ItemStack stack = player.getInventory().getItem(held.getNewSlot());
            return stack == null || stack.getType().isAir() ? null : stack;
        }
        if (!(entity instanceof LivingEntity living)) return null;
        EntityEquipment equipment = living.getEquipment();
        if (equipment == null) return null;
        ItemStack stack = equipment.getItemInMainHand();
        return stack.getType().isAir() ? null : stack;
    }

    public static void updateHeldItem(LivingEntity living, Cancellable event, ItemStack stack) {
        ItemStack updated = stack == null || stack.getAmount() <= 0 ? null : stack;
        if (living instanceof Player player && event instanceof PlayerItemHeldEvent held && player.equals(held.getPlayer())) {
            player.getInventory().setItem(held.getNewSlot(), updated);
            return;
        }
        EntityEquipment equipment = living.getEquipment();
        if (equipment != null) equipment.setItemInMainHand(updated);
    }

    public static String get(ItemStack stack, String name) {
        NamespacedKey key = key(name);
        if (stack == null || key == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static boolean set(ItemStack stack, String name, String value) {
        NamespacedKey key = key(name);
        if (stack == null || key == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        if (value == null) meta.getPersistentDataContainer().remove(key);
        else meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        stack.setItemMeta(meta);
        return true;
    }
}

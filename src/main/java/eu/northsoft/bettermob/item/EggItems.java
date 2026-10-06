package eu.northsoft.bettermob.item;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.mob.MobDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class EggItems {
    public static final Material FALLBACK = Material.ZOMBIE_SPAWN_EGG;

    private final NamespacedKey key;

    public EggItems(BetterMobPlugin plugin) {
        this.key = new NamespacedKey(plugin, "egg_mob");
    }

    public static Material materialFor(EntityType type) {
        Material material = Material.getMaterial(type.name() + "_SPAWN_EGG");
        return material == null ? FALLBACK : material;
    }

    public ItemStack create(MobDefinition definition, int amount) {
        Material material = definition.egg == null || definition.egg.material() == null ? null : Material.getMaterial(definition.egg.material());
        ItemStack stack = new ItemStack(material == null ? materialFor(definition.type) : material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        String name = definition.egg != null && definition.egg.name() != null ? definition.egg.name() : definition.displayName;
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("BetterMob: " + definition.id).decoration(TextDecoration.ITALIC, false)
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, definition.id);
        stack.setItemMeta(meta);
        return stack;
    }

    public String mobIdOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}

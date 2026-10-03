package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.ai.AiGoalApplier;
import eu.northsoft.bettermob.item.ItemDefinition;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class EquipMechanic implements Mechanic {
    private final SkillEngine engine;

    public EquipMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        if (!(call.target().entity() instanceof LivingEntity living) || living.getEquipment() == null) return;
        String spec = firstParam(p, "item", "i", "type");
        if (spec == null) return;
        String[] parts = spec.trim().split(":", 2);

        ItemStack stack;
        ItemDefinition custom = engine.items().get(parts[0]);
        if (custom != null) {
            stack = engine.items().create(custom, 1);
        } else {
            Material material = Material.matchMaterial(parts[0]);
            if (material == null || !material.isItem()) {
                engine.plugin().messages().warn("skill.equipUnknown", "value", parts[0]);
                return;
            }
            stack = new ItemStack(material);
        }

        EquipmentSlot slot = switch ((parts.length > 1 ? parts[1] : "hand").trim().toLowerCase(Locale.ROOT)) {
            case "offhand", "off_hand" -> EquipmentSlot.OFF_HAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HAND;
        };
        EntityEquipment equipment = living.getEquipment();
        equipment.setItem(slot, stack);

        if (living instanceof Mob mob) {
            equipment.setDropChance(slot, 0f);
            MobDefinition definition = engine.mobManager().definitionOf(mob.getUniqueId());
            if (definition != null && !definition.aiGoalSelectors.isEmpty()) AiGoalApplier.promoteRanged(mob);
        }
    }
}

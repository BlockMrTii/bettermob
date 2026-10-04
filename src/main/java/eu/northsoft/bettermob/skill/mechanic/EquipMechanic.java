package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.ai.AiGoalApplier;
import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

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

        ItemStack stack = EquipmentSupport.itemFor(engine.items(), parts[0]);
        if (stack == null) {
            engine.plugin().messages().warn("skill.equipUnknown", "value", parts[0]);
            return;
        }
        EquipmentSlot slot = EquipmentSupport.slotFor(parts.length > 1 ? parts[1] : "hand");
        EquipmentSupport.equip(living, slot, stack);
        if (living instanceof Mob mob) {
            MobDefinition definition = engine.mobManager().definitionOf(mob.getUniqueId());
            if (definition != null && !definition.aiGoalSelectors.isEmpty()) AiGoalApplier.promoteRanged(mob);
        }
    }
}

package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.inventory.ItemStack;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class HoldingCondition implements SkillCondition {
    private final SkillEngine engine;

    public HoldingCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        if (context.caster().getEquipment() == null) return false;
        ItemStack held = context.caster().getEquipment().getItemInMainHand();
        String wanted = conditionParam(paramsRaw, "m", "material", "i", "item", "type");
        if (wanted.isEmpty() || held == null) return false;
        for (String name : wanted.split(",")) {
            String trimmed = name.trim();
            if (trimmed.equalsIgnoreCase(held.getType().name()) || trimmed.equalsIgnoreCase(engine.items().idOf(held))) return true;
        }
        return false;
    }
}

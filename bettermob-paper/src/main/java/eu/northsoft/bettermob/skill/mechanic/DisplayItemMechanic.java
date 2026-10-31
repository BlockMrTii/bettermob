package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class DisplayItemMechanic implements Mechanic {
    private final SkillEngine engine;

    public DisplayItemMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        String id = firstParam(call.params(), "i", "item");
        if (!(entity instanceof ItemDisplay display) || id == null) return;
        ItemStack stack = EquipmentSupport.itemFor(engine.items(), id.trim());
        if (stack == null) {
            engine.plugin().messages().warn("skill.displayItemUnknown", "item", id);
            return;
        }
        display.setItemStack(stack);
    }
}

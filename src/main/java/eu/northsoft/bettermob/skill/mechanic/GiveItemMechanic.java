package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class GiveItemMechanic implements Mechanic {
    private static final int MAX_AMOUNT = 64;

    private final SkillEngine engine;

    public GiveItemMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof Player player)) return;
        String id = firstParam(call.params(), "i", "item", "type");
        if (id == null) return;
        ItemStack stack = EquipmentSupport.itemFor(engine.items(), id.trim());
        if (stack == null) {
            engine.plugin().messages().warn("skill.giveItemUnknown", "item", id);
            return;
        }
        stack.setAmount(Math.max(1, Math.min(MAX_AMOUNT, parseInt(firstParam(call.params(), "a", "amount"), 1))));
        for (ItemStack left : player.getInventory().addItem(stack).values()) player.getWorld().dropItem(player.getLocation(), left);
    }
}

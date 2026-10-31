package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class TakeItemMechanic implements Mechanic {
    private final SkillEngine engine;

    public TakeItemMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        if (!(call.target().entity() instanceof Player player)) return;
        String itemId = firstParam(p, "i", "item", "type");
        if (itemId == null) return;
        int remaining = Math.max(1, parseInt(firstParam(p, "a", "amount"), 1));

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !itemId.trim().equalsIgnoreCase(engine.items().idOf(stack))) continue;
            int taken = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - taken);
            player.getInventory().setItem(slot, stack.getAmount() <= 0 ? null : stack);
            remaining -= taken;
        }
    }
}

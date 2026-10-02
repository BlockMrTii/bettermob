package eu.northsoft.bettermob;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

final class ItemListener implements Listener {
    private final ItemRegistry items;
    private final SkillEngine skillEngine;

    ItemListener(ItemRegistry items, SkillEngine skillEngine) {
        this.items = items;
        this.skillEngine = skillEngine;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemDefinition definition = items.definitionOf(event.getItem());
        if (definition == null) return;

        SkillContext context = new SkillContext(event.getPlayer(), event.getPlayer(), event);
        for (MobDefinition.SkillTrigger trigger : definition.skillTriggers) {
            if (trigger.trigger() == MobDefinition.SkillTrigger.Trigger.USE) skillEngine.runStep(trigger.step(), context);
        }
    }
}

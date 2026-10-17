package eu.northsoft.bettermob.item;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static eu.northsoft.bettermob.mob.MobDefinition.SkillTrigger.Trigger;

public final class ItemListener implements Listener {
    private final ItemRegistry items;
    private final SkillEngine skillEngine;

    public ItemListener(ItemRegistry items, SkillEngine skillEngine) {
        this.items = items;
        this.skillEngine = skillEngine;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        fire(event.getPlayer(), event.getItem(), Trigger.USE, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;
        fire(event.getPlayer(), event.getPlayer().getInventory().getItemInMainHand(), Trigger.SWING, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPressF(PlayerSwapHandItemsEvent event) {
        fire(event.getPlayer(), event.getOffHandItem(), Trigger.PRESSF, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) fire(player, event.getItem().getItemStack(), Trigger.PICKUP, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        fire(event.getPlayer(), event.getItemDrop().getItemStack(), Trigger.DROP, event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEquip(PlayerItemHeldEvent event) {
        fire(event.getPlayer(), event.getPlayer().getInventory().getItem(event.getNewSlot()), Trigger.EQUIP, event);
    }

    private void fire(Player player, ItemStack stack, Trigger type, Cancellable event) {
        ItemDefinition definition = items.definitionOf(stack);
        if (definition == null) return;
        SkillContext context = new SkillContext(player, player, event);
        for (MobDefinition.SkillTrigger trigger : definition.skillTriggers) {
            if (trigger.trigger() == type) skillEngine.runStep(trigger.step(), context);
        }
    }
}

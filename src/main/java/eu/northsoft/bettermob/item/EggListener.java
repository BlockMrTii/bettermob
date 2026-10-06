package eu.northsoft.bettermob.item;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class EggListener implements Listener {
    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final EggItems eggs;

    public EggListener(BetterMobPlugin plugin, MobManager manager, EggItems eggs) {
        this.plugin = plugin;
        this.manager = manager;
        this.eggs = eggs;
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        String mobId = eggs.mobIdOf(event.getItem());
        if (mobId == null) return;
        if (event.useInteractedBlock() == Event.Result.DENY || event.useItemInHand() == Event.Result.DENY) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.OFF_HAND && eggs.mobIdOf(event.getPlayer().getInventory().getItemInMainHand()) != null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() == null) return;

        MobDefinition definition = manager.registry().get(mobId);
        Player player = event.getPlayer();
        if (definition == null) {
            plugin.messages().send(player, "egg.unknownMob", "mob", mobId);
            return;
        }
        Block clicked = event.getClickedBlock();
        Location at = clicked.getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        manager.spawn(definition, at);
        if (player.getGameMode() != GameMode.CREATIVE) consume(player, event.getHand());
    }

    @EventHandler
    public void onUseOnEntity(PlayerInteractEntityEvent event) {
        ItemStack held = event.getPlayer().getInventory().getItem(event.getHand());
        if (eggs.mobIdOf(held) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event) {
        if (eggs.mobIdOf(event.getItem()) != null) event.setCancelled(true);
    }

    private static void consume(Player player, EquipmentSlot hand) {
        ItemStack stack = player.getInventory().getItem(hand);
        if (stack == null) return;
        stack.setAmount(stack.getAmount() - 1);
        player.getInventory().setItem(hand, stack.getAmount() <= 0 ? null : stack);
    }
}

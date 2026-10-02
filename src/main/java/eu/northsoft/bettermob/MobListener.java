package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.event.BetterMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

final class MobListener implements Listener {
    private final MobManager manager;
    private final DropRegistry drops;

    MobListener(MobManager manager, DropRegistry drops) {
        this.manager = manager;
        this.drops = drops;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        // Tracker hier NICHT schliessen: der Entity-Body bleibt nach dem Tod noch ~20
        // Ticks fuer die Sterbeanimation da. Macht man das Modell schon jetzt weg,
        // sieht man stattdessen die Vanilla-Sterbeanimation. Das eigentliche Aufraeumen
        // passiert in onRemove, wenn der Body wirklich aus der Welt verschwindet.
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        if (definition == null) return;
        // Eigene Drops ersetzen die Vanilla-Drops (und -XP) - dafuer braucht es kein PreventOtherDrops.
        if (definition.options.preventOtherDrops() || definition.drops != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        if (definition.drops != null) {
            DropRegistry.Result result = drops.roll(definition.drops);
            event.getDrops().addAll(result.items());
            event.setDroppedExp(event.getDroppedExp() + result.exp());
        }

        Bukkit.getPluginManager().callEvent(new BetterMobDeathEvent(event.getEntity(), definition.toInfo()));
        manager.fireTrigger(event.getEntity(), definition, MobDefinition.SkillTrigger.Trigger.DEATH, null, null);
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        manager.release(event.getEntity());
    }

    /** Server-Neustart/Chunk-Reload: alte BetterMob-Entities haben Tracker/Timer verloren, hier wiederherstellen. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) manager.handleLoad(living);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageModifier(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        double modifier = manager.modifierFor(victim.getUniqueId(), event.getCause());
        if (modifier != 1.0) event.setDamage(event.getDamage() * modifier);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity source = event.getDamager();
        if (source instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            source = shooter instanceof Entity shooterEntity ? shooterEntity : null;
        }

        // ~onDamaged + ThreatTable: der Mob selbst wurde getroffen.
        if (event.getEntity() instanceof LivingEntity victimMob) {
            MobDefinition definition = manager.definitionOf(victimMob.getUniqueId());
            if (definition != null) {
                if (definition.threatTable && source instanceof LivingEntity attacker) {
                    manager.registerThreat(victimMob.getUniqueId(), attacker, event.getFinalDamage());
                }
                manager.fireTrigger(victimMob, definition, MobDefinition.SkillTrigger.Trigger.DAMAGED,
                        source instanceof LivingEntity living ? living : null, event);
            }
        }
        // ~onAttack: der Mob hat selbst zugeschlagen.
        // ~onAttack ist der Nahkampf. Ein Pfeil des Mobs zaehlt nicht: sonst bricht das uebliche
        // "CancelEvent ~onAttack" (Nahkampf unterbinden) auch den Schaden seiner eigenen Pfeile ab.
        if (source instanceof LivingEntity attackerMob && !manager.inSkillDamage() && !(event.getDamager() instanceof Projectile)) {
            MobDefinition definition = manager.definitionOf(attackerMob.getUniqueId());
            if (definition != null) manager.fireTrigger(attackerMob, definition, MobDefinition.SkillTrigger.Trigger.ATTACK,
                    event.getEntity() instanceof LivingEntity living ? living : null, event);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        MobDefinition definition = manager.definitionOf(event.getRightClicked().getUniqueId());
        if (definition == null) return;

        if (!definition.options.interactable()) {
            event.setCancelled(true);
            return;
        }
        if (event.getRightClicked() instanceof LivingEntity living) {
            manager.fireTrigger(living, definition, MobDefinition.SkillTrigger.Trigger.INTERACT, event.getPlayer(), event);
        }
        if (definition.options.preventRenaming() && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.NAME_TAG) {
            event.setCancelled(true);
        }
    }

    /** Rechtsklick auf einen Armor Stand tauscht sonst dessen Ausruestung - bei Interactable: false (z.B. Karten-Vorschau) verbieten. */
    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        MobDefinition definition = manager.definitionOf(event.getRightClicked().getUniqueId());
        if (definition != null && !definition.options.interactable()) event.setCancelled(true);
    }

    @EventHandler
    public void onLeash(PlayerLeashEntityEvent event) {
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        if (definition != null && definition.options.preventLeashing()) event.setCancelled(true);
    }
}

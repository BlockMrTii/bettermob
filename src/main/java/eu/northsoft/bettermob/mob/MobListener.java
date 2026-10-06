package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.api.event.BetterMobDamageEvent;
import eu.northsoft.bettermob.api.event.BetterMobDeathEvent;
import eu.northsoft.bettermob.drop.DropRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

import java.util.concurrent.ThreadLocalRandom;

public final class MobListener implements Listener {
    private final MobManager manager;
    private final DropRegistry drops;

    public MobListener(MobManager manager, DropRegistry drops) {
        this.manager = manager;
        this.drops = drops;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        LivingEntity entity = event.getEntity();
        if (manager.definitionOf(entity.getUniqueId()) != null) return;
        Location at = entity.getLocation();
        String biome = at.getWorld().getBiome(at).getKey().toString();
        for (MobDefinition definition : manager.registry().all().values()) {
            SpawnRule rule = definition.spawnRule;
            if (rule == null || definition.type != entity.getType()) continue;
            if (!rule.matches(at.getWorld().getName(), biome, at.getWorld().getTime(), ThreadLocalRandom.current().nextDouble())) continue;
            event.setCancelled(true);
            manager.spawn(definition, at);
            return;
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        if (definition == null) return;

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

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        manager.removeLeftoverHelpers(event.getEntities());
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) manager.handleLoad(living);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamageModifier(EntityDamageEvent event) {
        if (event.getEntity() instanceof LivingEntity victim) {
            double modifier = manager.modifierFor(victim.getUniqueId(), event.getCause());
            if (modifier != 1.0) event.setDamage(event.getDamage() * modifier);
        }
        if (event instanceof EntityDamageByEntityEvent byEntity && !byEntity.isCancelled()) fireDamageApi(byEntity);
    }

    private void fireDamageApi(EntityDamageByEntityEvent event) {
        if (BetterMobDamageEvent.getHandlerList().getRegisteredListeners().length == 0) return;
        Entity source = event.getDamager();
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) source = shooter;
        if (event.getEntity() instanceof LivingEntity victim && fireDamage(victim, source, true, event)) return;
        if (source instanceof LivingEntity attacker) fireDamage(attacker, event.getEntity(), false, event);
    }

    private boolean fireDamage(LivingEntity mob, Entity other, boolean mobIsVictim, EntityDamageByEntityEvent event) {
        MobDefinition definition = manager.definitionOf(mob.getUniqueId());
        if (definition == null) return false;
        BetterMobDamageEvent api = new BetterMobDamageEvent(mob, definition.toInfo(), other, mobIsVictim, event.getCause(), event.getDamage());
        Bukkit.getPluginManager().callEvent(api);
        if (api.isCancelled()) {
            event.setCancelled(true);
            return true;
        }
        if (api.getDamage() != event.getDamage()) event.setDamage(api.getDamage());
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFactionTarget(EntityTargetLivingEntityEvent event) {
        if (event.getTarget() != null && manager.sameFaction(event.getEntity(), event.getTarget())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFactionDamage(EntityDamageByEntityEvent event) {
        Entity source = event.getDamager();
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) source = shooter;
        if (manager.sameFaction(source, event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        Entity source = event.getDamager();
        if (source instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            source = shooter instanceof Entity shooterEntity ? shooterEntity : null;
        }

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

        if (source instanceof LivingEntity attackerMob && !manager.inSkillDamage() && !(event.getDamager() instanceof Projectile)) {
            MobDefinition definition = manager.definitionOf(attackerMob.getUniqueId());
            if (definition != null) manager.fireTrigger(attackerMob, definition, MobDefinition.SkillTrigger.Trigger.ATTACK,
                    event.getEntity() instanceof LivingEntity living ? living : null, event);
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        if (definition == null) return;
        LivingEntity target = event.getEntity() instanceof Mob mob ? mob.getTarget() : null;
        manager.fireTrigger(event.getEntity(), definition, MobDefinition.SkillTrigger.Trigger.SHOOT, target, event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        MobDefinition definition = manager.definitionOf(event.getRightClicked().getUniqueId());
        if (definition == null) return;

        if (!definition.options.interactable()) {
            event.setCancelled(true);
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND && event.getRightClicked() instanceof LivingEntity living) {
            manager.fireTrigger(living, definition, MobDefinition.SkillTrigger.Trigger.INTERACT, event.getPlayer(), event);
        }
        if (definition.options.preventRenaming() && event.getPlayer().getInventory().getItem(event.getHand()).getType() == Material.NAME_TAG) {
            event.setCancelled(true);
        }
    }

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

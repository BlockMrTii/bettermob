package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.api.event.mob.BetterMobDamageEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobDeathEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobDespawnEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobDropEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobKillEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobTargetEvent;
import eu.northsoft.bettermob.drop.DropRegistry;
import eu.northsoft.bettermob.skill.SkillTags;
import eu.northsoft.bettermob.stats.KillStats;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.projectiles.ProjectileSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class MobListener implements Listener {
    private static final Set<EntityRemoveEvent.Cause> REMOVED_WITHOUT_DYING = EnumSet.of(
            EntityRemoveEvent.Cause.DESPAWN, EntityRemoveEvent.Cause.DISCARD, EntityRemoveEvent.Cause.PLUGIN);

    private final MobManager manager;
    private final DropRegistry drops;
    private final KillStats kills;

    public MobListener(MobManager manager, DropRegistry drops, KillStats kills) {
        this.manager = manager;
        this.drops = drops;
        this.kills = kills;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        LivingEntity entity = event.getEntity();
        if (manager.definitionOf(entity.getUniqueId()) != null) return;
        Location at = entity.getLocation();
        String biome = at.getWorld().getBiome(at).getKey().toString();
        Set<String> regionsHere = null;
        for (MobDefinition definition : manager.registry().all().values()) {
            SpawnRule rule = definition.spawnRule;
            if (rule == null || rule.action() != SpawnRule.Action.REPLACE || definition.type != entity.getType()) continue;
            if (rule.usesRegions() && regionsHere == null) regionsHere = manager.regions().at(at);
            if (!rule.matches(at.getWorld().getName(), biome, at.getWorld().getTime(), ThreadLocalRandom.current().nextDouble(), regionsHere == null ? Set.of() : regionsHere)) continue;
            if (!manager.regions().spawnAllowed(at)) return;
            event.setCancelled(true);
            manager.spawn(definition, at);
            return;
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity killerEntity = resolveKiller(event.getEntity());
        fireKill(event.getEntity());
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        if (definition == null) {
            manager.fireEntityAura(event.getEntity(), "ondeath", killerEntity, event);
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer != null) kills.record(killer.getUniqueId(), killer.getName(), definition.id);

        if (manager.optionsOf(event.getEntity(), definition).preventOtherDrops() || definition.drops != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        if (definition.drops != null) {
            DropRegistry.Result result = drops.roll(definition.drops);
            event.getDrops().addAll(result.items());
            event.setDroppedExp(event.getDroppedExp() + result.exp());
        }

        if (BetterMobDropEvent.getHandlerList().getRegisteredListeners().length > 0) {
            BetterMobDropEvent dropEvent = new BetterMobDropEvent(event.getEntity(), definition.toInfo(), killer, event.getDrops(), event.getDroppedExp());
            Bukkit.getPluginManager().callEvent(dropEvent);
            event.setDroppedExp(Math.max(0, dropEvent.getExp()));
        }
        if (BetterMobDeathEvent.getHandlerList().getRegisteredListeners().length > 0) {
            Bukkit.getPluginManager().callEvent(new BetterMobDeathEvent(event.getEntity(), definition.toInfo()));
        }
        manager.fireTrigger(event.getEntity(), definition, MobDefinition.SkillTrigger.Trigger.DEATH, killerEntity, event);
    }

    private static LivingEntity resolveKiller(LivingEntity victim) {
        if (victim.getLastDamageCause() instanceof EntityDamageByEntityEvent byEntity) {
            Entity damager = byEntity.getDamager();
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) damager = shooter;
            if (damager instanceof LivingEntity living && !living.equals(victim)) return living;
        }
        return victim.getKiller();
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (event.getEntity() instanceof LivingEntity living && REMOVED_WITHOUT_DYING.contains(event.getCause())) {
            MobDefinition definition = manager.definitionOf(living.getUniqueId());
            if (definition != null) {
                if (BetterMobDespawnEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    Bukkit.getPluginManager().callEvent(new BetterMobDespawnEvent(living, definition.toInfo(), event.getCause()));
                }
                manager.fireTrigger(living, definition, MobDefinition.SkillTrigger.Trigger.DESPAWN, null, null);
            }
        }
        manager.release(event.getEntity());
    }

    @EventHandler
    public void onAddToWorld(EntityAddToWorldEvent event) {
        if (event.getEntity() instanceof LivingEntity living) manager.handleCopied(living);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        manager.removeLeftoverHelpers(event.getEntities());
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) manager.handleLoad(living);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEffectDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager().getScoreboardTags().contains(SkillTags.EFFECT_TAG)) event.setCancelled(true);
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
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter && Bukkit.isOwnedByCurrentRegion(shooter)) source = shooter;
        if (!Bukkit.isOwnedByCurrentRegion(source)) return;
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

    private void fireKill(LivingEntity victim) {
        if (!(victim.getLastDamageCause() instanceof EntityDamageByEntityEvent byEntity)) return;
        Entity damager = byEntity.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) damager = shooter;
        if (!(damager instanceof LivingEntity killer) || killer.equals(victim)) return;
        MobDefinition definition = manager.definitionOf(killer.getUniqueId());
        if (definition == null) return;
        if (BetterMobKillEvent.getHandlerList().getRegisteredListeners().length > 0 && Bukkit.isOwnedByCurrentRegion(killer)) {
            Bukkit.getPluginManager().callEvent(new BetterMobKillEvent(killer, definition.toInfo(), victim));
        }
        manager.fireTrigger(killer, definition, MobDefinition.SkillTrigger.Trigger.KILL, victim, null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHealthLoss(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        MobDefinition definition = manager.definitionOf(mob.getUniqueId());
        if (definition != null && (manager.hasHealthTriggers(definition) || definition.hasPhases())) {
            manager.checkHealth(mob, definition, mob.getHealth(), Math.max(0, mob.getHealth() - event.getFinalDamage()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHealthGain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        MobDefinition definition = manager.definitionOf(mob.getUniqueId());
        if (definition != null && (manager.hasHealthTriggers(definition) || definition.hasPhases())) {
            manager.checkHealth(mob, definition, mob.getHealth(), mob.getHealth() + event.getAmount());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTargetChanged(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        MobDefinition definition = manager.definitionOf(mob.getUniqueId());
        if (definition != null) manager.targetChanged(mob, definition, event.getTarget());
    }

    @EventHandler(ignoreCancelled = true)
    public void onTargetApi(EntityTargetLivingEntityEvent event) {
        if (BetterMobTargetEvent.getHandlerList().getRegisteredListeners().length == 0) return;
        if (!(event.getEntity() instanceof LivingEntity mob)) return;
        MobDefinition definition = manager.definitionOf(mob.getUniqueId());
        if (definition == null) return;
        LivingEntity old = mob instanceof Mob living ? living.getTarget() : null;
        BetterMobTargetEvent api = new BetterMobTargetEvent(mob, definition.toInfo(), old, event.getTarget());
        Bukkit.getPluginManager().callEvent(api);
        if (api.isCancelled()) event.setCancelled(true);
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
            source = shooter instanceof Entity shooterEntity && Bukkit.isOwnedByCurrentRegion(shooterEntity) ? shooterEntity : null;
        }

        if (event.getEntity() instanceof LivingEntity victimMob) {
            MobDefinition definition = manager.definitionOf(victimMob.getUniqueId());
            LivingEntity attacker = source instanceof LivingEntity living ? living : null;
            if (definition != null) {
                if (definition.threatTable && attacker != null) {
                    manager.registerThreat(victimMob.getUniqueId(), attacker, event.getFinalDamage());
                }
                if (attacker != null) manager.alertGroup(victimMob, definition, attacker);
                manager.fireTrigger(victimMob, definition, MobDefinition.SkillTrigger.Trigger.DAMAGED, attacker, event);
            } else {
                manager.fireEntityAura(victimMob, "ondamaged", attacker, event);
            }
        }

        if (source instanceof LivingEntity attackerMob && !manager.inSkillDamage() && !(event.getDamager() instanceof Projectile)) {
            MobDefinition definition = manager.definitionOf(attackerMob.getUniqueId());
            LivingEntity victim = event.getEntity() instanceof LivingEntity living ? living : null;
            if (definition != null) manager.fireTrigger(attackerMob, definition, MobDefinition.SkillTrigger.Trigger.ATTACK, victim, event);
            else manager.fireEntityAura(attackerMob, "onattack", victim, event);
        }
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        LivingEntity target = event.getEntity() instanceof Mob mob ? mob.getTarget() : null;
        if (definition != null) manager.fireTrigger(event.getEntity(), definition, MobDefinition.SkillTrigger.Trigger.SHOOT, target, event);
        else manager.fireEntityAura(event.getEntity(), "onshoot", target, event);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        MobDefinition definition = manager.definitionOf(event.getRightClicked().getUniqueId());
        if (definition == null) return;

        if (!manager.optionsOf(event.getRightClicked(), definition).interactable()) {
            event.setCancelled(true);
            return;
        }
        if (event.getHand() == EquipmentSlot.HAND && event.getRightClicked() instanceof LivingEntity living) {
            manager.fireTrigger(living, definition, MobDefinition.SkillTrigger.Trigger.INTERACT, event.getPlayer(), event);
        }
        if (manager.optionsOf(event.getRightClicked(), definition).preventRenaming() && event.getPlayer().getInventory().getItem(event.getHand()).getType() == Material.NAME_TAG) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        MobDefinition definition = manager.definitionOf(event.getRightClicked().getUniqueId());
        if (definition != null && !manager.optionsOf(event.getRightClicked(), definition).interactable()) event.setCancelled(true);
    }

    @EventHandler
    public void onLeash(PlayerLeashEntityEvent event) {
        MobDefinition definition = manager.definitionOf(event.getEntity().getUniqueId());
        if (definition != null && manager.optionsOf(event.getEntity(), definition).preventLeashing()) event.setCancelled(true);
    }
}

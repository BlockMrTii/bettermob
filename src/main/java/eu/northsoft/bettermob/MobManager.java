package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.event.BetterMobSpawnEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.ref.WeakReference;

final class MobManager {
    private final BetterMobPlugin plugin;
    private final MobRegistry registry;
    private final BetterModelHook betterModel;
    private final ModelEngineHook modelEngine;
    private final ItemRegistry items;
    private final Map<UUID, Object> trackers = new ConcurrentHashMap<>();

    private final Map<UUID, Object> modelEngineTrackers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Threat>> threatTables = new ConcurrentHashMap<>();
    private final Map<UUID, Map<DamageCause, Double>> damageModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, MobDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<UUID, List<Runnable>> timers = new ConcurrentHashMap<>();
    private SkillEngine skillEngine;

    final NamespacedKey mobIdKey;

    MobManager(BetterMobPlugin plugin, MobRegistry registry, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
        this.mobIdKey = new NamespacedKey(plugin, "mob_id");
    }

    LivingEntity spawn(MobDefinition definition, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.type);
        entity.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(definition.displayName));
        entity.setCustomNameVisible(definition.options.alwaysShowName());

        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(definition.health);
            entity.setHealth(definition.health);
        }
        var damage = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damage != null) damage.setBaseValue(definition.damage);
        var speed = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null && definition.options.movementSpeed() >= 0) speed.setBaseValue(definition.options.movementSpeed());

        entity.setCollidable(definition.options.collidable());
        entity.setSilent(definition.options.silent());
        entity.setInvulnerable(definition.options.invincible());
        entity.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, definition.id);
        applyAppearanceOptions(entity, definition);

        if (definition.removeAi) entity.setAI(false);
        if (entity instanceof Mob mob) AiGoalApplier.apply(mob, definition.aiGoalSelectors, definition.aiTargetSelectors, plugin, other -> definitions.containsKey(other.getUniqueId()));
        if (definition.threatTable) threatTables.put(entity.getUniqueId(), new ConcurrentHashMap<>());
        if (!definition.damageModifiers.isEmpty()) damageModifiers.put(entity.getUniqueId(), definition.damageModifiers);

        if (definition.options.preventSunburn()) {
            if (entity instanceof Zombie zombie) zombie.setShouldBurnInDay(false);
            if (entity instanceof AbstractSkeleton skeleton) skeleton.setShouldBurnInDay(false);
        }

        if (!hasModelSkill(definition, MobDefinition.SkillTrigger.Trigger.SPAWN)) {
            Object tracker = betterModel.attachIfPresent(entity, definition.modelId);
            if (tracker != null) {
                trackers.put(entity.getUniqueId(), tracker);

                entity.setInvisible(true);
            }
        }

        definitions.put(entity.getUniqueId(), definition);
        fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.SPAWN, null, null);
        scheduleTimers(entity, definition);
        Bukkit.getPluginManager().callEvent(new BetterMobSpawnEvent(entity, definition.toInfo()));
        return entity;
    }

    private void applyAppearanceOptions(LivingEntity entity, MobDefinition definition) {
        MobDefinition.Options options = definition.options;
        if (options.invisible()) entity.setInvisible(true);
        if (!options.canMove()) {
            entity.setAI(false);
            entity.setGravity(false);
        }
        if (options.marker() && entity instanceof ArmorStand stand) stand.setMarker(true);
        if (options.knockbackResistance() >= 0) {
            var knockback = entity.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
            if (knockback != null) knockback.setBaseValue(options.knockbackResistance());
        }
        if (options.followRange() >= 0) {
            var follow = entity.getAttribute(Attribute.FOLLOW_RANGE);
            if (follow != null) follow.setBaseValue(options.followRange());
        }
        if (options.scale() >= 0) {
            var scale = entity.getAttribute(Attribute.SCALE);
            if (scale != null) scale.setBaseValue(options.scale());
        }
        if (options.preventItemPickup()) entity.setCanPickupItems(false);
        if (options.itemHead() != null) {
            ItemDefinition item = items.get(options.itemHead());
            EntityEquipment equipment = entity.getEquipment();
            if (item == null) {
                plugin.getLogger().warning("Mob '" + definition.id + "': ItemHead-Item '" + options.itemHead() + "' ist nicht registriert.");
            } else if (equipment != null) {
                equipment.setHelmet(items.create(item, 1));

                if (entity instanceof Mob) equipment.setHelmetDropChance(0f);
            }
        }
    }

    private boolean staysInvisible(Entity entity) {
        MobDefinition definition = definitions.get(entity.getUniqueId());
        return definition != null && definition.options.invisible();
    }

    void release(Entity entity) {
        if (skillEngine != null) skillEngine.forget(entity.getUniqueId());
        Object tracker = trackers.remove(entity.getUniqueId());
        if (tracker != null) betterModel.close(tracker);
        Object modelEngineTracker = modelEngineTrackers.remove(entity.getUniqueId());
        if (modelEngineTracker != null) modelEngine.close(modelEngineTracker);
        threatTables.remove(entity.getUniqueId());
        damageModifiers.remove(entity.getUniqueId());
        definitions.remove(entity.getUniqueId());
        List<Runnable> cancellers = timers.remove(entity.getUniqueId());
        if (cancellers != null) cancellers.forEach(Runnable::run);
    }

    boolean inSkillDamage() {
        return skillEngine != null && skillEngine.isApplyingDamage();
    }

    void setSkillEngine(SkillEngine skillEngine) {
        this.skillEngine = skillEngine;
    }

    Object trackerFor(UUID entityId) {
        return trackers.get(entityId);
    }

    void replaceTracker(Entity entity, Object newTracker) {
        Object old = trackers.remove(entity.getUniqueId());
        if (old != null) betterModel.close(old);
        if (newTracker != null) trackers.put(entity.getUniqueId(), newTracker);
        entity.setInvisible(newTracker != null || staysInvisible(entity));
    }

    void replaceModelEngineTracker(Entity entity, Object newTracker) {
        Object old = modelEngineTrackers.remove(entity.getUniqueId());
        if (old != null) modelEngine.close(old);
        if (newTracker != null) modelEngineTrackers.put(entity.getUniqueId(), newTracker);
        entity.setInvisible(newTracker != null || staysInvisible(entity));
    }

    void fireTrigger(LivingEntity entity, MobDefinition definition, MobDefinition.SkillTrigger.Trigger type,
                      LivingEntity trigger, Cancellable event) {
        if (skillEngine == null) return;
        String auraKind = switch (type) {
            case DAMAGED -> "ondamaged";
            case ATTACK -> "onattack";
            case DEATH -> "ondeath";
            case SHOOT -> "onshoot";
            default -> null;
        };
        if (auraKind != null) skillEngine.fireAuras(entity, auraKind, trigger, event);
        SkillContext context = new SkillContext(entity, trigger, event);
        for (MobDefinition.SkillTrigger skillTrigger : definition.skillTriggers) {
            if (skillTrigger.trigger() == type) skillEngine.runStep(skillTrigger.step(), context);
        }
    }

    private void scheduleTimers(LivingEntity entity, MobDefinition definition) {
        if (definition.threatTable && entity instanceof Mob mob) {
            timers.computeIfAbsent(mob.getUniqueId(), key -> new ArrayList<>())
                    .add(Tasks.runTimer(plugin, mob, 20L, 20L, () -> retarget(mob)));
        }
        for (MobDefinition.SkillTrigger trigger : definition.skillTriggers) {
            if (trigger.trigger() != MobDefinition.SkillTrigger.Trigger.TIMER) continue;
            Runnable cancel = Tasks.runTimer(plugin, entity, trigger.timerTicks(), trigger.timerTicks(), () -> {
                if (!entity.isValid()) return;
                skillEngine.runStep(trigger.step(), SkillContext.of(entity));
            });
            timers.computeIfAbsent(entity.getUniqueId(), key -> new ArrayList<>()).add(cancel);
        }
    }

    String factionOf(Entity entity) {
        MobDefinition definition = definitions.get(entity.getUniqueId());
        return definition == null || definition.faction == null ? null : definition.faction.toLowerCase(java.util.Locale.ROOT);
    }

    boolean inFaction(Entity entity, String faction) {
        if (faction == null) return false;
        String own = factionOf(entity);
        if (own != null) return own.equals(faction);
        if (!(entity instanceof org.bukkit.entity.Player player)) return false;
        if (player.hasPermission(factionPermission(faction))) return true;
        for (String entry : plugin.getConfig().getStringList("factions." + faction)) {
            if (entry.equalsIgnoreCase(player.getName()) || entry.equalsIgnoreCase(player.getUniqueId().toString())) return true;
        }
        return false;
    }

    private final java.util.Set<String> registeredFactionPermissions = ConcurrentHashMap.newKeySet();

    private String factionPermission(String faction) {
        String node = "bettermob.faction." + faction;
        if (registeredFactionPermissions.add(node) && Bukkit.getPluginManager().getPermission(node) == null) {
            Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(node, org.bukkit.permissions.PermissionDefault.FALSE));
        }
        return node;
    }

    boolean sameFaction(Entity first, Entity second) {
        String faction = factionOf(first);
        if (faction != null) return inFaction(second, faction);
        faction = factionOf(second);
        return faction != null && inFaction(first, faction);
    }

    MobDefinition definitionOf(UUID entityId) {
        return definitions.get(entityId);
    }

    void handleLoad(LivingEntity entity) {
        if (definitions.containsKey(entity.getUniqueId())) return;
        String id = entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (id == null) return;
        MobDefinition definition = registry.get(id);
        if (definition == null) return;

        definitions.put(entity.getUniqueId(), definition);
        if (definition.threatTable) threatTables.put(entity.getUniqueId(), new ConcurrentHashMap<>());
        if (!definition.damageModifiers.isEmpty()) damageModifiers.put(entity.getUniqueId(), definition.damageModifiers);

        if (!hasModelSkill(definition, MobDefinition.SkillTrigger.Trigger.LOAD)) {
            Object tracker = betterModel.attachIfPresent(entity, definition.modelId);
            if (tracker != null) {
                trackers.put(entity.getUniqueId(), tracker);
                entity.setInvisible(true);
            }
        }

        scheduleTimers(entity, definition);
        fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.LOAD, null, null);
    }

    private boolean hasModelSkill(MobDefinition definition, MobDefinition.SkillTrigger.Trigger trigger) {
        for (MobDefinition.SkillTrigger skillTrigger : definition.skillTriggers) {
            if (skillTrigger.trigger() == trigger && skillTrigger.step() instanceof SkillStep.Mechanic mechanic
                    && mechanic.name().equals("model")) {
                return true;
            }
        }
        return false;
    }

    double modifierFor(UUID entityId, DamageCause cause) {
        Map<DamageCause, Double> modifiers = damageModifiers.get(entityId);
        return modifiers == null ? 1.0 : modifiers.getOrDefault(cause, 1.0);
    }

    private static final class Threat {
        final WeakReference<LivingEntity> attacker;
        double amount;

        Threat(LivingEntity attacker) {
            this.attacker = new WeakReference<>(attacker);
        }
    }

    void registerThreat(UUID mobId, LivingEntity attacker, double amount) {
        Map<UUID, Threat> table = threatTables.get(mobId);
        if (table != null) table.computeIfAbsent(attacker.getUniqueId(), id -> new Threat(attacker)).amount += amount;
    }

    private void retarget(Mob mob) {
        Map<UUID, Threat> table = threatTables.get(mob.getUniqueId());
        if (table == null || !mob.isValid()) return;

        LivingEntity top = null;
        double best = -1;
        var iterator = table.values().iterator();
        while (iterator.hasNext()) {
            Threat threat = iterator.next();
            LivingEntity attacker = threat.attacker.get();
            if (attacker == null || attacker.isDead() || !attacker.getWorld().equals(mob.getWorld())) {
                iterator.remove();
                continue;
            }
            if (threat.amount > best) {
                best = threat.amount;
                top = attacker;
            }
        }
        if (top != null) mob.setTarget(top);
    }

    MobRegistry registry() {
        return registry;
    }
}

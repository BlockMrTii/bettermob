package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.ai.AiGoalApplier;
import eu.northsoft.bettermob.ai.BehaviourGoals;
import eu.northsoft.bettermob.api.event.BetterMobSpawnEvent;
import eu.northsoft.bettermob.integration.PlaceholderHook;
import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.item.ItemDefinition;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.skill.SkillTags;
import eu.northsoft.bettermob.util.Tasks;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobManager {
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
    private final BossBarManager bossBars;
    private SkillEngine skillEngine;

    private static final long HOME_CHECK_TICKS = 40L;

    public final NamespacedKey mobIdKey;

    public MobManager(BetterMobPlugin plugin, MobRegistry registry, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
        this.mobIdKey = new NamespacedKey(plugin, "mob_id");
        this.bossBars = new BossBarManager(plugin);
    }

    public LivingEntity spawn(MobDefinition definition, Location location) {
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, definition.type);
        applyDefinition(entity, definition, true);

        if (entity instanceof Mob mob) AiGoalApplier.apply(mob, definition.aiGoalSelectors, definition.aiTargetSelectors, plugin, other -> definitions.containsKey(other.getUniqueId()));
        applyBehaviour(entity, definition);
        if (definition.threatTable) threatTables.put(entity.getUniqueId(), new ConcurrentHashMap<>());
        if (!definition.damageModifiers.isEmpty()) damageModifiers.put(entity.getUniqueId(), definition.damageModifiers);

        if (!hasModelSkill(definition, MobDefinition.SkillTrigger.Trigger.SPAWN)) {
            attachModelField(entity, definition.modelId);
        }

        definitions.put(entity.getUniqueId(), definition);
        fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.SPAWN, null, null);
        scheduleTimers(entity, definition);
        bossBars.attach(entity, definition);
        Bukkit.getPluginManager().callEvent(new BetterMobSpawnEvent(entity, definition.toInfo()));
        return entity;
    }

    private void applyDefinition(LivingEntity entity, MobDefinition definition, boolean spawning) {
        entity.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderHook.apply(null, definition.displayName)));
        entity.setCustomNameVisible(definition.options.alwaysShowName());

        var maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(definition.health);
            entity.setHealth(spawning ? definition.health : Math.min(entity.getHealth(), definition.health));
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

        preventSunburn(entity, definition);
        applyEquipment(entity, definition);
    }

    private NamespacedKey homeKey() {
        return new NamespacedKey(plugin, "home");
    }

    public Location homeOf(Entity entity) {
        String stored = entity.getPersistentDataContainer().get(homeKey(), PersistentDataType.STRING);
        if (stored == null) return null;
        String[] parts = stored.split(";");
        if (parts.length != 3 && parts.length != 4) return null;
        try {
            int offset = parts.length - 3;
            World world = offset == 0 ? entity.getWorld() : Bukkit.getWorld(UUID.fromString(parts[0]));
            if (world == null) return null;
            return new Location(world, Double.parseDouble(parts[offset]), Double.parseDouble(parts[offset + 1]), Double.parseDouble(parts[offset + 2]));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void applyBehaviour(LivingEntity entity, MobDefinition definition) {
        Behaviour behaviour = definition.behaviour;
        if (behaviour != null && behaviour.needsHome() && homeOf(entity) == null) {
            Location at = entity.getLocation();
            entity.getPersistentDataContainer().set(homeKey(), PersistentDataType.STRING, at.getWorld().getUID() + ";" + at.getX() + ";" + at.getY() + ";" + at.getZ());
        }
        if (entity instanceof Mob mob) BehaviourGoals.apply(plugin, mob, behaviour, () -> homeOf(entity));
    }

    private void applyEquipment(LivingEntity entity, MobDefinition definition) {
        if (definition.equipment.isEmpty() || entity.getEquipment() == null) return;
        for (String line : definition.equipment) {
            String[] parts = line.trim().split(":", 2);
            ItemStack stack = EquipmentSupport.itemFor(items, parts[0]);
            if (stack == null) {
                plugin.messages().warn("mob.equipmentUnknownItem", "mob", definition.id, "value", parts[0]);
                continue;
            }
            String slotName = parts.length > 1 ? parts[1] : "hand";
            if (!EquipmentSupport.isKnownSlot(slotName)) {
                plugin.messages().warn("mob.equipmentUnknownSlot", "mob", definition.id, "value", slotName);
                continue;
            }
            EquipmentSupport.equip(entity, EquipmentSupport.slotFor(slotName), stack);
        }
        if (entity instanceof Mob mob && !definition.aiGoalSelectors.isEmpty()) AiGoalApplier.promoteRanged(mob);
    }

    private static void preventSunburn(LivingEntity entity, MobDefinition definition) {
        if (!definition.options.preventSunburn()) return;
        if (entity instanceof Zombie zombie) zombie.setShouldBurnInDay(false);
        if (entity instanceof AbstractSkeleton skeleton) skeleton.setShouldBurnInDay(false);
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
                plugin.messages().warn("mob.itemHeadMissing", "mob", definition.id, "item", options.itemHead());
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

    public void release(Entity entity) {
        if (skillEngine != null) skillEngine.forget(entity.getUniqueId());
        bossBars.detach(entity.getUniqueId());
        Object tracker = trackers.remove(entity.getUniqueId());
        if (tracker != null) betterModel.close(tracker);
        Object modelEngineTracker = modelEngineTrackers.remove(entity.getUniqueId());
        if (modelEngineTracker != null) modelEngine.close(modelEngineTracker);
        threatTables.remove(entity.getUniqueId());
        damageModifiers.remove(entity.getUniqueId());
        definitions.remove(entity.getUniqueId());
        inCombat.remove(entity.getUniqueId());
        exitCombatToken.remove(entity.getUniqueId());
        List<Runnable> cancellers = timers.remove(entity.getUniqueId());
        if (cancellers != null) cancellers.forEach(Runnable::run);
    }

    public void shutdown() {
        bossBars.clear();
    }

    public boolean inSkillDamage() {
        return skillEngine != null && skillEngine.isApplyingDamage();
    }

    public void setSkillEngine(SkillEngine skillEngine) {
        this.skillEngine = skillEngine;
    }

    private void attachModelField(Entity entity, String modelId) {
        Object tracker = betterModel.attachIfPresent(entity, modelId);
        if (tracker != null) {
            replaceTracker(entity, tracker);
            return;
        }
        Object engineTracker = modelEngine.attachIfPresent(entity, modelId);
        if (engineTracker != null) replaceModelEngineTracker(entity, engineTracker);
    }

    public Object modelEngineTrackerFor(UUID entityId) {
        return modelEngineTrackers.get(entityId);
    }

    public Object trackerFor(UUID entityId) {
        return trackers.get(entityId);
    }

    public void replaceTracker(Entity entity, Object newTracker) {
        Object old = trackers.remove(entity.getUniqueId());
        if (old != null) betterModel.close(old);
        if (newTracker != null) {
            trackers.put(entity.getUniqueId(), newTracker);
            Object engineOld = modelEngineTrackers.remove(entity.getUniqueId());
            if (engineOld != null) modelEngine.close(engineOld);
        }
        entity.setInvisible(hasModel(entity) || staysInvisible(entity));
    }

    private boolean hasModel(Entity entity) {
        return trackers.containsKey(entity.getUniqueId()) || modelEngineTrackers.containsKey(entity.getUniqueId());
    }

    public void replaceModelEngineTracker(Entity entity, Object newTracker) {
        Object old = modelEngineTrackers.remove(entity.getUniqueId());
        if (old != null) modelEngine.close(old);
        if (newTracker != null) {
            modelEngineTrackers.put(entity.getUniqueId(), newTracker);
            Object betterOld = trackers.remove(entity.getUniqueId());
            if (betterOld != null) betterModel.close(betterOld);
        }
        entity.setInvisible(hasModel(entity) || staysInvisible(entity));
    }

    public void fireTrigger(LivingEntity entity, MobDefinition definition, MobDefinition.SkillTrigger.Trigger type,
                      LivingEntity trigger, Cancellable event) {
        fireTriggers(entity, definition, type, definition.triggersOf(type), trigger, event);
    }

    private void fireTriggers(LivingEntity entity, MobDefinition definition, MobDefinition.SkillTrigger.Trigger type,
                              List<MobDefinition.SkillTrigger> triggers, LivingEntity trigger, Cancellable event) {
        if (skillEngine == null) return;
        if (!Bukkit.isOwnedByCurrentRegion(entity)) {
            LivingEntity original = trigger;
            Tasks.runOwned(plugin, entity, () -> fireTriggers(entity, definition, type, triggers, original, null));
            return;
        }
        if (trigger != null && !Bukkit.isOwnedByCurrentRegion(trigger)) trigger = null;
        if (plugin.debug().info()) plugin.debug().info("trigger " + type + " on " + definition.id + (trigger != null ? " (by " + trigger.getName() + ")" : ""), definition.id);
        String auraKind = switch (type) {
            case DAMAGED -> "ondamaged";
            case ATTACK -> "onattack";
            case DEATH -> "ondeath";
            case SHOOT -> "onshoot";
            default -> null;
        };
        if (auraKind != null) skillEngine.fireAuras(entity, auraKind, trigger, event);
        SkillContext context = new SkillContext(entity, trigger, event);
        for (MobDefinition.SkillTrigger skillTrigger : triggers) {
            skillEngine.runStep(skillTrigger.step(), context);
        }
        if (event != null && event.isCancelled()) plugin.debug().info("trigger " + type + " on " + definition.id + ": event cancelled", definition.id);
    }

    private final Map<UUID, Object> exitCombatToken = new ConcurrentHashMap<>();
    private final Set<UUID> inCombat = ConcurrentHashMap.newKeySet();
    private static final long EXIT_COMBAT_TICKS = 100L;

    public boolean hasHealthTriggers(MobDefinition definition) {
        return !definition.triggersOf(MobDefinition.SkillTrigger.Trigger.HEALTH).isEmpty();
    }

    public void checkHealth(LivingEntity mob, MobDefinition definition, double before, double health) {
        List<MobDefinition.SkillTrigger> triggers = definition.triggersOf(MobDefinition.SkillTrigger.Trigger.HEALTH);
        if (triggers.isEmpty() || health <= 0) return;
        var attribute = mob.getAttribute(Attribute.MAX_HEALTH);
        double max = attribute == null ? Math.max(before, health) : attribute.getValue();
        for (MobDefinition.SkillTrigger trigger : triggers) {
            if (trigger.health() == null) continue;
            if (trigger.health().matches(health, max) && !trigger.health().matches(before, max)) {
                fireTriggers(mob, definition, MobDefinition.SkillTrigger.Trigger.HEALTH, List.of(trigger), null, null);
            }
        }
    }

    public void targetChanged(LivingEntity mob, MobDefinition definition, LivingEntity target) {
        UUID id = mob.getUniqueId();
        if (target != null) {
            exitCombatToken.remove(id);
            fireTrigger(mob, definition, MobDefinition.SkillTrigger.Trigger.TARGET, target, null);
            if (inCombat.add(id)) fireTrigger(mob, definition, MobDefinition.SkillTrigger.Trigger.ENTERCOMBAT, target, null);
            return;
        }
        fireTrigger(mob, definition, MobDefinition.SkillTrigger.Trigger.LOSETARGET, null, null);
        if (!inCombat.contains(id)) return;
        Object token = new Object();
        exitCombatToken.put(id, token);
        Tasks.runLater(plugin, mob, EXIT_COMBAT_TICKS, () -> {
            if (exitCombatToken.remove(id, token) && mob instanceof Mob living && living.getTarget() == null && inCombat.remove(id)) {
                fireTrigger(mob, definition, MobDefinition.SkillTrigger.Trigger.EXITCOMBAT, null, null);
            }
        });
    }

    private void scheduleTimers(LivingEntity entity, MobDefinition definition) {
        if (definition.threatTable && entity instanceof Mob mob) {
            timers.computeIfAbsent(mob.getUniqueId(), key -> new ArrayList<>())
                    .add(Tasks.runTimer(plugin, mob, 20L, 20L, () -> retarget(mob)));
        }
        if (definition.behaviour != null && definition.behaviour.hasHomeLimit()) {
            double limit = definition.behaviour.maxHomeDistance();
            timers.computeIfAbsent(entity.getUniqueId(), key -> new ArrayList<>()).add(Tasks.runTimer(plugin, entity, HOME_CHECK_TICKS, HOME_CHECK_TICKS, () -> {
                Location home = homeOf(entity);
                if (home == null || !home.getWorld().equals(entity.getWorld()) || entity.getLocation().distanceSquared(home) <= limit * limit) return;
                if (plugin.debug().info()) plugin.debug().info("mob " + definition.id + " is further than " + limit + " blocks from home, teleporting it back", definition.id);
                entity.teleportAsync(home);
            }));
        }
        for (MobDefinition.SkillTrigger trigger : definition.triggersOf(MobDefinition.SkillTrigger.Trigger.TIMER)) {
            Runnable cancel = Tasks.runTimer(plugin, entity, trigger.timerTicks(), trigger.timerTicks(), () -> {
                if (!entity.isValid()) return;
                skillEngine.runStep(trigger.step(), SkillContext.of(entity));
            });
            timers.computeIfAbsent(entity.getUniqueId(), key -> new ArrayList<>()).add(cancel);
        }
    }

    public int reloadLiving() {
        int updated = 0;
        Set<String> removed = new HashSet<>();
        for (Map.Entry<UUID, MobDefinition> entry : List.copyOf(definitions.entrySet())) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living)) continue;
            MobDefinition old = entry.getValue();
            MobDefinition fresh = registry.get(old.id);
            if (fresh == null) {
                if (removed.add(old.id)) plugin.messages().warn("mob.reloadRemoved", "mob", old.id);
                continue;
            }
            updated++;
            Tasks.runLater(plugin, living, 1L, () -> rebind(living, fresh));
        }
        return updated;
    }

    private void rebind(LivingEntity entity, MobDefinition fresh) {
        UUID id = entity.getUniqueId();
        MobDefinition old = definitions.get(id);
        if (old == null || !entity.isValid()) return;

        List<Runnable> cancellers = timers.remove(id);
        if (cancellers != null) cancellers.forEach(Runnable::run);
        if (skillEngine != null) skillEngine.forget(id);
        inCombat.remove(id);
        exitCombatToken.remove(id);

        definitions.put(id, fresh);
        if (fresh.threatTable) threatTables.putIfAbsent(id, new ConcurrentHashMap<>());
        else threatTables.remove(id);
        if (fresh.damageModifiers.isEmpty()) damageModifiers.remove(id);
        else damageModifiers.put(id, fresh.damageModifiers);

        if ((old.removeAi || !old.options.canMove()) && fresh.options.canMove() && !fresh.removeAi) {
            entity.setAI(true);
            entity.setGravity(true);
        }
        applyDefinition(entity, fresh, false);
        entity.setInvisible(trackers.containsKey(id) || modelEngineTrackers.containsKey(id) || fresh.options.invisible());
        if (entity instanceof Mob mob) {
            AiGoalApplier.apply(mob, changedSelectors(old.aiGoalSelectors, fresh.aiGoalSelectors),
                    changedSelectors(old.aiTargetSelectors, fresh.aiTargetSelectors), plugin, other -> definitions.containsKey(other.getUniqueId()));
        }
        applyBehaviour(entity, fresh);

        if (!hasModelSkill(fresh, MobDefinition.SkillTrigger.Trigger.LOAD)) {
            attachModelField(entity, fresh.modelId);
        }
        scheduleTimers(entity, fresh);
        bossBars.attach(entity, fresh);
        fireTrigger(entity, fresh, MobDefinition.SkillTrigger.Trigger.LOAD, null, null);
    }

    private static List<String> changedSelectors(List<String> before, List<String> after) {
        if (before.equals(after)) return List.of();
        return after.stream().anyMatch(line -> line.trim().equalsIgnoreCase("clear")) ? after : List.of();
    }

    public int killAll(String mobId, World world) {
        int removed = 0;
        for (Map.Entry<UUID, MobDefinition> entry : List.copyOf(definitions.entrySet())) {
            if (mobId != null && !entry.getValue().id.equalsIgnoreCase(mobId)) continue;
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null || (world != null && !entity.getWorld().equals(world))) continue;
            Tasks.runLater(plugin, entity, 1L, entity::remove);
            removed++;
        }
        if (mobId == null && !Tasks.FOLIA) {
            for (World target : world != null ? List.of(world) : Bukkit.getWorlds()) {
                removeLeftoverHelpers(target.getEntitiesByClass(ArmorStand.class));
            }
        }
        return removed;
    }

    public void removeLeftoverHelpers(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof ArmorStand stand && stand.getScoreboardTags().contains(SkillTags.HELPER_TAG)) {
                Tasks.runLater(plugin, stand, 1L, stand::remove);
            }
        }
    }

    public Map<String, Integer> aliveCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (MobDefinition definition : definitions.values()) counts.merge(definition.id, 1, Integer::sum);
        return counts;
    }

    public int timerCount() {
        return timers.values().stream().mapToInt(List::size).sum();
    }

    public int aliveCount() {
        return definitions.size();
    }

    public int aliveCount(String id) {
        int count = 0;
        for (MobDefinition definition : definitions.values()) {
            if (definition.id.equalsIgnoreCase(id)) count++;
        }
        return count;
    }

    public String factionOf(Entity entity) {
        MobDefinition definition = definitions.get(entity.getUniqueId());
        return definition == null || definition.faction == null ? null : definition.faction.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean inFaction(Entity entity, String faction) {
        if (faction == null) return false;
        String own = factionOf(entity);
        if (own != null) return own.equals(faction);
        if (!(entity instanceof org.bukkit.entity.Player player)) return false;
        if (player.hasPermission(factionPermission(faction))) return true;
        Set<String> members = factionMembers().get(faction);
        return members != null && (members.contains(player.getName().toLowerCase(java.util.Locale.ROOT)) || members.contains(player.getUniqueId().toString()));
    }

    private record FactionCache(org.bukkit.configuration.file.FileConfiguration source, Map<String, Set<String>> members) {}

    private volatile FactionCache factionCache = new FactionCache(null, Map.of());

    private Map<String, Set<String>> factionMembers() {
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();
        FactionCache cache = factionCache;
        if (config != cache.source()) {
            Map<String, Set<String>> loaded = new java.util.HashMap<>();
            var section = config.getConfigurationSection("factions");
            if (section != null) {
                for (String name : section.getKeys(false)) {
                    Set<String> names = new HashSet<>();
                    for (String entry : section.getStringList(name)) names.add(entry.toLowerCase(java.util.Locale.ROOT));
                    loaded.put(name.toLowerCase(java.util.Locale.ROOT), names);
                }
            }
            cache = new FactionCache(config, loaded);
            factionCache = cache;
        }
        return cache.members();
    }

    private final java.util.Set<String> registeredFactionPermissions = ConcurrentHashMap.newKeySet();

    private String factionPermission(String faction) {
        String node = "bettermob.faction." + faction;
        if (registeredFactionPermissions.contains(node)) return node;
        synchronized (registeredFactionPermissions) {
            if (registeredFactionPermissions.add(node) && Bukkit.getPluginManager().getPermission(node) == null) {
                Bukkit.getPluginManager().addPermission(new org.bukkit.permissions.Permission(node, org.bukkit.permissions.PermissionDefault.FALSE));
            }
        }
        return node;
    }

    public boolean sameFaction(Entity first, Entity second) {
        String faction = factionOf(first);
        if (faction != null) return inFaction(second, faction);
        faction = factionOf(second);
        return faction != null && inFaction(first, faction);
    }

    public MobDefinition definitionOf(UUID entityId) {
        return definitions.get(entityId);
    }

    public void handleLoad(LivingEntity entity) {
        if (definitions.containsKey(entity.getUniqueId())) return;
        String id = entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
        if (id == null) return;
        MobDefinition definition = registry.get(id);
        if (definition == null) return;

        definitions.put(entity.getUniqueId(), definition);
        if (definition.threatTable) threatTables.put(entity.getUniqueId(), new ConcurrentHashMap<>());
        if (!definition.damageModifiers.isEmpty()) damageModifiers.put(entity.getUniqueId(), definition.damageModifiers);

        if (entity instanceof Mob mob) {
            AiGoalApplier.apply(mob, definition.aiGoalSelectors, definition.aiTargetSelectors, plugin, other -> definitions.containsKey(other.getUniqueId()));
        }
        applyBehaviour(entity, definition);
        preventSunburn(entity, definition);

        if (!hasModelSkill(definition, MobDefinition.SkillTrigger.Trigger.LOAD)) {
            attachModelField(entity, definition.modelId);
        }

        scheduleTimers(entity, definition);
        bossBars.attach(entity, definition);
        fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.LOAD, null, null);
    }

    private boolean hasModelSkill(MobDefinition definition, MobDefinition.SkillTrigger.Trigger trigger) {
        for (MobDefinition.SkillTrigger skillTrigger : definition.triggersOf(trigger)) {
            if (skillTrigger.step() instanceof SkillStep.Mechanic mechanic && (mechanic.name().equals("model") || mechanic.name().equals("modelengine"))) {
                return true;
            }
        }
        return false;
    }

    public double modifierFor(UUID entityId, DamageCause cause) {
        Map<DamageCause, Double> modifiers = damageModifiers.get(entityId);
        return modifiers == null ? 1.0 : modifiers.getOrDefault(cause, 1.0);
    }

    private static final class Threat {
        public final WeakReference<LivingEntity> attacker;
        public double amount;

        public Threat(LivingEntity attacker) {
            this.attacker = new WeakReference<>(attacker);
        }
    }

    public void registerThreat(UUID mobId, LivingEntity attacker, double amount) {
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

    public MobRegistry registry() {
        return registry;
    }
}

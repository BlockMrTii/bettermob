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
    // Getrennt von trackers: jede Engine hat ihre eigene close()-Methode, ein gemischter
    // Pool wuerde beim Aufraeumen versuchen, z.B. ein ModelEngine-Objekt ueber BetterModels
    // close() zu schliessen (und umgekehrt) - das schlaegt fehl und lasst es als Leak liegen.
    private final Map<UUID, Object> modelEngineTrackers = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, Threat>> threatTables = new ConcurrentHashMap<>();
    private final Map<UUID, Map<DamageCause, Double>> damageModifiers = new ConcurrentHashMap<>();
    private final Map<UUID, MobDefinition> definitions = new ConcurrentHashMap<>();
    private final Map<UUID, List<Runnable>> timers = new ConcurrentHashMap<>();
    private SkillEngine skillEngine;

    // Nur die Mob-ID muss als NBT am Entity haengen - die braucht man nach einem Neustart/
    // Chunk-Reload, um die Definition ueberhaupt wiederzufinden (handleLoad). Alle anderen
    // Flags (PreventOtherDrops etc.) stehen schon in der im RAM gehaltenen MobDefinition;
    // die zusaetzlich als eigene PDC-Eintraege zu fuehren hiesse, bei JEDEM Damage-/
    // Interact-/Leash-Event server-weit (auch fuer Entities, die gar keine BetterMob-Mobs
    // sind) unnoetig NBT zu lesen, statt einmal im schon vorhandenen HashMap nachzuschauen.
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
        if (entity instanceof Mob mob) AiGoalApplier.apply(mob, definition.aiGoalSelectors, definition.aiTargetSelectors, plugin.getLogger());
        if (definition.threatTable) threatTables.put(entity.getUniqueId(), new ConcurrentHashMap<>());
        if (!definition.damageModifiers.isEmpty()) damageModifiers.put(entity.getUniqueId(), definition.damageModifiers);

        if (definition.options.preventSunburn()) {
            if (entity instanceof Zombie zombie) zombie.setShouldBurnInDay(false);
            if (entity instanceof AbstractSkeleton skeleton) skeleton.setShouldBurnInDay(false);
        }

        // Packs wie nogs_menagerie haengen ihr Modell selbst per "model{}"-Skill an
        // ~onSpawn/~onLoad an (fuer benannte Teile/mehrere Modelle). Haengen wir hier
        // zusaetzlich automatisch ueber "Model:" an, schliesst der Skill-Mechanic den
        // gerade erst erzeugten Tracker im selben Tick wieder und erstellt sofort einen
        // neuen - dieses Schliessen-und-Neuerstellen auf demselben Entity im selben Tick
        // laesst BetterModel nichts mehr rendern. Also nur automatisch anhaengen, wenn
        // kein eigener Skill das ohnehin uebernimmt.
        if (!hasModelSkill(definition, MobDefinition.SkillTrigger.Trigger.SPAWN)) {
            Object tracker = betterModel.attachIfPresent(entity, definition.modelId);
            if (tracker != null) {
                trackers.put(entity.getUniqueId(), tracker);
                // BetterModel loescht das Vanilla-Aussehen nicht von selbst - ohne das hier
                // steht das Modell einfach zusaetzlich zum voll sichtbaren Pig/Zombie/etc. da
                // und man sieht effektiv nur noch den Vanilla-Mob.
                entity.setInvisible(true);
            }
        }

        definitions.put(entity.getUniqueId(), definition);
        fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.SPAWN, null, null);
        scheduleTimers(entity, definition);
        Bukkit.getPluginManager().callEvent(new BetterMobSpawnEvent(entity, definition.toInfo()));
        return entity;
    }

    /** Invisible/CanMove/Marker/ItemHead/Knockback - alles, was nur Aussehen und Beweglichkeit der Huelle betrifft. */
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
        if (options.itemHead() != null) {
            ItemDefinition item = items.get(options.itemHead());
            EntityEquipment equipment = entity.getEquipment();
            if (item == null) {
                plugin.getLogger().warning("Mob '" + definition.id + "': ItemHead-Item '" + options.itemHead() + "' ist nicht registriert.");
            } else if (equipment != null) {
                equipment.setHelmet(items.create(item, 1));
                // Drop-Chancen gibt es nur bei Mobs - bei Armor Stands wirft Paper eine Exception.
                if (entity instanceof Mob) equipment.setHelmetDropChance(0f);
            }
        }
    }

    /** Ob die Huelle auch ohne Modell unsichtbar bleiben soll (Options.Invisible). */
    private boolean staysInvisible(Entity entity) {
        MobDefinition definition = definitions.get(entity.getUniqueId());
        return definition != null && definition.options.invisible();
    }

    void release(Entity entity) {
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

    /** True, solange gerade die damage-Mechanic eines Skills Schaden austeilt. */
    boolean inSkillDamage() {
        return skillEngine != null && skillEngine.isApplyingDamage();
    }

    void setSkillEngine(SkillEngine skillEngine) {
        this.skillEngine = skillEngine;
    }

    Object trackerFor(UUID entityId) {
        return trackers.get(entityId);
    }

    /** Fuer den "model"-Skill-Mechanic: altes Modell schliessen, neues merken, Vanilla-Optik passend ein-/ausblenden. */
    void replaceTracker(Entity entity, Object newTracker) {
        Object old = trackers.remove(entity.getUniqueId());
        if (old != null) betterModel.close(old);
        if (newTracker != null) trackers.put(entity.getUniqueId(), newTracker);
        entity.setInvisible(newTracker != null || staysInvisible(entity));
    }

    /** Pendant zu replaceTracker(), aber fuer den "modelengine"-Skill-Mechanic. */
    void replaceModelEngineTracker(Entity entity, Object newTracker) {
        Object old = modelEngineTrackers.remove(entity.getUniqueId());
        if (old != null) modelEngine.close(old);
        if (newTracker != null) modelEngineTrackers.put(entity.getUniqueId(), newTracker);
        entity.setInvisible(newTracker != null || staysInvisible(entity));
    }

    /** Fuehrt alle Skills aus, die der Mob fuer diesen Trigger-Typ registriert hat. */
    void fireTrigger(LivingEntity entity, MobDefinition definition, MobDefinition.SkillTrigger.Trigger type,
                      LivingEntity trigger, Cancellable event) {
        if (skillEngine == null) return;
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

    MobDefinition definitionOf(UUID entityId) {
        return definitions.get(entityId);
    }

    /**
     * Ein Chunk mit einem alten BetterMob-Entity wird geladen (Neustart, Chunk-Reload):
     * Tracker/Threat-Table/Timer waren nur im Arbeitsspeicher und sind weg, hier werden
     * sie anhand der am Entity gespeicherten Mob-ID wiederhergestellt. Bereits bekannte
     * Entities (z.B. gerade erst gespawnt) werden uebersprungen.
     */
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

    /** True, wenn ein eigener "model{}"-Skill fuer diesen Trigger bereits existiert - dann
     *  soll die automatische "Model:"-Anheftung nicht zusaetzlich dagegenlaufen. */
    private boolean hasModelSkill(MobDefinition definition, MobDefinition.SkillTrigger.Trigger trigger) {
        for (MobDefinition.SkillTrigger skillTrigger : definition.skillTriggers) {
            if (skillTrigger.trigger() == trigger && skillTrigger.step() instanceof SkillStep.Mechanic mechanic
                    && mechanic.name().equals("model")) {
                return true;
            }
        }
        return false;
    }

    /** Fuer Options.DamageModifiers: Schaden dieser Ursache mit dem konfigurierten Faktor skalieren. */
    double modifierFor(UUID entityId, DamageCause cause) {
        Map<DamageCause, Double> modifiers = damageModifiers.get(entityId);
        return modifiers == null ? 1.0 : modifiers.getOrDefault(cause, 1.0);
    }

    /** Gesammelter Schaden eines Angreifers; die Referenz ist schwach, damit Tote nicht im Speicher haengen. */
    private static final class Threat {
        final WeakReference<LivingEntity> attacker;
        double amount;

        Threat(LivingEntity attacker) {
            this.attacker = new WeakReference<>(attacker);
        }
    }

    /** Fuer Modules.ThreatTable: Schaden sammeln, statt des Verursachers wird spaeter das hoechste Ziel angegriffen. */
    void registerThreat(UUID mobId, LivingEntity attacker, double amount) {
        Map<UUID, Threat> table = threatTables.get(mobId);
        if (table != null) table.computeIfAbsent(attacker.getUniqueId(), id -> new Threat(attacker)).amount += amount;
    }

    /** Alle 20 Ticks pro Mob (auf dessen eigenem Thread): auf das aktuell gefaehrlichste Ziel umlenken. */
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

package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.CustomMechanic;
import eu.northsoft.bettermob.api.MechanicContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fuehrt SkillDefinitions aus - ein kleiner, auf die tatsaechlich gebrauchten Mechaniken
 * beschraenkter Nachbau von MythicMobs' Skill-System. Baut nur die Mechaniken/Conditions
 * ein, die im Pack vorkommen; unbekannte Mechanics/Conditions werden geloggt statt den
 * Server zum Absturz zu bringen.
 */
final class SkillEngine implements org.bukkit.event.Listener {
    private final BetterMobPlugin plugin;
    private final SkillRegistry registry;
    private final MobManager mobManager;
    private final BetterModelHook betterModel;
    private final ModelEngineHook modelEngine;
    private final ItemRegistry items;
    private final Map<UUID, Long> gcdUntilMillis = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    // Aktive Auren pro Entity: Name -> Ablaufzeit in ms (Long.MAX_VALUE = ohne Ende).
    private final Map<UUID, Map<String, Aura>> auras = new ConcurrentHashMap<>();
    private final Map<String, List<SkillStep>> inlineSkills = new ConcurrentHashMap<>();
    // Gesetzt, solange die damage-Mechanic Schaden austeilt: dieser Schaden soll kein ~onAttack
    // ausloesen, sonst bricht dessen CancelEvent den Schaden des Skills selbst wieder ab.
    private final ThreadLocal<Boolean> applyingDamage = ThreadLocal.withInitial(() -> false);
    private final Map<String, CustomMechanicEntry> customMechanics = new ConcurrentHashMap<>();

    private static final Set<String> BUILTIN_MECHANICS = Set.of("cancelskill", "cancelevent", "skill", "look", "sound",
            "state", "potion", "breakblock", "gcd", "model", "modelengine", "randomskill", "remove", "command",
            "summon", "mountmodel", "delay", "effect:particles", "e:p", "particles", "effect:particlering", "spin", "takeitem", "sudoskill", "damage", "throw", "lunge", "setblock", "equip", "aura", "ondamaged", "onattack", "ontick", "ondeath",
            "onshoot", "bodyrotation", "addtag", "removetag", "ignite", "totem", "velocity", "freeze", "shoot", "stun", "setnodamageticks");

    private record CustomMechanicEntry(Plugin owner, CustomMechanic mechanic) {}

    SkillEngine(BetterMobPlugin plugin, SkillRegistry registry, MobManager mobManager, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.mobManager = mobManager;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** Der Wurf eines Skills darf nicht ueber den Tod hinaus wirken: Respawnende starten ohne Restgeschwindigkeit. */
    @org.bukkit.event.EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setVelocity(new Vector());
        Tasks.runLater(plugin, player, 1L, () -> player.setVelocity(new Vector()));
    }

    /** Pfeil eines shoot{oh=[...]}: die Treffer-Zeilen laufen mit dem Schuetzen als Caster und dem Getroffenen als Ziel. */
    private record PendingShot(LivingEntity shooter, List<SkillStep> onHit) {}

    private final Map<UUID, PendingShot> shots = new ConcurrentHashMap<>();

    @org.bukkit.event.EventHandler
    public void onProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        PendingShot shot = shots.remove(event.getEntity().getUniqueId());
        if (shot == null) return;
        if (event.getHitEntity() instanceof LivingEntity hit && !hit.equals(shot.shooter())) {
            executeSteps(shot.onHit(), 0, new SkillContext(shot.shooter(), hit, null).withTrigger(hit));
        }
    }

    /**
     * shoot{type=arrow;velocity;damage;oh=[...]}: schiesst einen Pfeil auf den Ausloeser bzw. das Ziel des Mobs.
     * ponytail: nur Pfeile; Geschwindigkeit = velocity*2 (grob wie Vanilla-Bogen), Schaden = damage-Param.
     */
    private void shoot(SkillContext context, Map<String, String> p) {
        LivingEntity caster = context.caster();
        LivingEntity aim = context.trigger();
        if (aim == null && caster instanceof Mob mob) aim = mob.getTarget();
        if (aim == null) return;
        double speed = Math.max(0.1, parseFloat(p.get("velocity"), 1f) * 2);
        Vector direction = aim.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector()).normalize();
        double damage = parseFloat(p.get("damage"), 2f);
        caster.launchProjectile(org.bukkit.entity.Arrow.class, direction.multiply(speed), arrow -> {
            arrow.setDamage(damage / speed);
            arrow.setPickupStatus(org.bukkit.entity.AbstractArrow.PickupStatus.DISALLOWED);
            String hit = p.get("oh");
            if (hit != null) {
                shots.put(arrow.getUniqueId(), new PendingShot(caster, inlineSkills.computeIfAbsent(hit, this::parseInline)));
                Tasks.runLater(plugin, arrow, 400L, () -> shots.remove(arrow.getUniqueId()));
            }
        });
    }

    private void freeze(Target target, Map<String, String> p) {
        if (target.entity() == null) return;
        int ticks = parseInt(firstParam(p, "ticks", "t", "d", "duration"), 140);
        target.entity().setFreezeTicks(Math.max(ticks, target.entity().getFreezeTicks()));
    }

    private void velocity(Target target, Map<String, String> p) {
        Entity entity = target.entity();
        if (entity == null) return;
        String mode = p.getOrDefault("m", p.getOrDefault("mode", "SET")).trim().toUpperCase(Locale.ROOT);
        Vector change = new Vector(parseFloat(p.get("x"), 0f), parseFloat(p.get("y"), 0f), parseFloat(p.get("z"), 0f));
        long interval = Math.max(1, parseInt(firstParam(p, "repeatinterval", "ri"), 1));
        applyVelocity(entity, mode, change);
        int repeat = parseInt(p.get("repeat"), 0);
        for (int i = 1; i <= repeat; i++) {
            Tasks.runLater(plugin, entity, i * interval, () -> applyVelocity(entity, mode, change));
        }
    }

    private void applyVelocity(Entity entity, String mode, Vector change) {
        Vector current = entity.getVelocity();
        entity.setVelocity(switch (mode) {
            case "ADD" -> current.add(change);
            case "MULTIPLY" -> current.multiply(change);
            case "DIVIDE" -> new Vector(div(current.getX(), change.getX()), div(current.getY(), change.getY()), div(current.getZ(), change.getZ()));
            default -> change.clone();
        });
    }

    private static double div(double value, double divisor) {
        return divisor == 0 ? value : value / divisor;
    }

    /** stun{d;ai;g;f}: d Ticks lang KI aus (ai, Standard true), ohne Schwerkraft (g) und/oder jeden Tick auf Geschwindigkeit 0 (f). */
    private void stun(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof Mob mob)) return;
        int ticks = parseInt(firstParam(p, "d", "duration", "t"), 20);
        boolean ai = !"false".equalsIgnoreCase(p.get("ai"));
        boolean gravity = "true".equalsIgnoreCase(p.get("g"));
        boolean freeze = "true".equalsIgnoreCase(p.get("f"));

        boolean hadGravity = mob.hasGravity();
        if (ai) mob.setAware(false);
        if (gravity) mob.setGravity(false);
        Runnable cancelFreeze = freeze
                ? Tasks.runTimer(plugin, mob, 1L, 1L, () -> mob.setVelocity(new Vector()))
                : () -> { };
        Tasks.runLater(plugin, mob, ticks, () -> {
            cancelFreeze.run();
            if (ai) mob.setAware(true);
            if (gravity) mob.setGravity(hadGravity);
        });
    }

    boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic) {
        String key = name.toLowerCase(Locale.ROOT);
        if (BUILTIN_MECHANICS.contains(key)) return false;
        return customMechanics.putIfAbsent(key, new CustomMechanicEntry(owner, mechanic)) == null;
    }

    void unregisterMechanic(String name) {
        customMechanics.remove(name.toLowerCase(Locale.ROOT));
    }

    void unregisterMechanics(Plugin owner) {
        customMechanics.values().removeIf(entry -> entry.owner().equals(owner));
    }

    void runById(String skillId, SkillContext context) {
        SkillDefinition skill = registry.get(skillId);
        if (skill == null) {
            plugin.getLogger().warning("Skill '" + skillId + "' ist nicht registriert.");
            return;
        }
        run(skill, context);
    }

    void run(SkillDefinition skill, SkillContext context) {
        if (check(skill.conditions, context, null) != Check.PASS) return;
        if (!skill.targetConditions.isEmpty()) {
            Target obstructing = resolve("obstructingblock", Map.of(), context);
            if (check(skill.targetConditions, context, obstructing) != Check.PASS) return;
        }
        if (skill.cooldown > 0 && !acquireSkillCooldown(context.caster(), skill)) return;
        executeSteps(skill.steps, 0, context);
    }

    private enum Check { PASS, FAIL, REDIRECTED }

    /**
     * Prueft eine Bedingungsliste. Eine Bedingung mit "castinstead <skill>" startet bei Erfuellung
     * stattdessen jenen Skill (der aktuelle bricht ab), sonst wird sie uebergangen; jede andere
     * nicht erfuellte Bedingung bricht den Skill ab.
     */
    private Check check(List<String> conditions, SkillContext context, Target target) {
        for (String raw : conditions) {
            Condition condition = parseCondition(raw);
            if (condition == null) continue;
            boolean met = evaluate(condition, context, target) == (condition.expected() == null || condition.expected());
            if ("castinstead".equals(condition.action())) {
                if (!met) continue;
                if (condition.actionValue() != null) runById(condition.actionValue(), context);
                return Check.REDIRECTED;
            }
            if (!met) return Check.FAIL;
        }
        return Check.PASS;
    }

    private boolean acquireSkillCooldown(LivingEntity caster, SkillDefinition skill) {
        long now = System.currentTimeMillis();
        String key = skillCooldownKey(caster, skill.id);
        Long until = cooldowns.get(key);
        if (until != null && until > now) return false;
        if (cooldowns.size() > 2048) cooldowns.values().removeIf(time -> time <= now);
        cooldowns.put(key, now + (long) (skill.cooldown * 1000));
        return true;
    }

    private boolean skillOnCooldown(LivingEntity caster, String skillId) {
        Long until = cooldowns.get(skillCooldownKey(caster, skillId));
        return until != null && until > System.currentTimeMillis();
    }

    private static String skillCooldownKey(LivingEntity caster, String skillId) {
        return caster.getUniqueId() + "@" + skillId.toLowerCase(Locale.ROOT);
    }

    /** Fuehrt einen einzelnen Schritt aus - fuer Mob-level "Skills:"-Zeilen, die direkt
     *  eine Mechanic statt einen Skill-Verweis sind (z.B. "sound{...} @self ~onDamaged"). */
    void runStep(SkillStep step, SkillContext context) {
        executeSteps(List.of(step), 0, context);
    }

    private void executeSteps(List<SkillStep> steps, int index, SkillContext context) {
        // Kein Existenz-Check hier am Einstieg: run()/runStep() kommen immer direkt aus
        // einem frischen Event/Trigger, der Caster lebt zu dem Zeitpunkt garantiert noch.
        // Der teure serverweite Bukkit.getEntity()-Lookup lohnt sich nur dort, wo wirklich
        // Zeit vergangen sein kann - also erst beim Wiederaufnehmen nach "delay" unten.
        for (int i = index; i < steps.size(); i++) {
            SkillStep step = steps.get(i);
            if (step instanceof SkillStep.Delay delay) {
                int next = i + 1;
                Tasks.runLater(plugin, context.caster(), delay.ticks(), () -> executeSteps(steps, next, context));
                return;
            }
            if (step instanceof SkillStep.Mechanic mechanic && runMechanic(mechanic, context)) return;
        }
    }

    /** Liefert true, wenn "cancelskill" gefeuert hat - dann bricht der Skill komplett ab. */
    private boolean runMechanic(SkillStep.Mechanic mechanic, SkillContext context) {
        Map<String, String> p = mechanic.params();

        // Inline-Bedingung am Zeilenende ("?cond{...}" / "?!cond{...}"): unbekannte
        // Bedingungen (Variablen/Factions gibt es bei uns nicht) gelten als erfuellt,
        // damit die Mechanic trotzdem laeuft statt komplett zu verschwinden.
        if (mechanic.inlineCondition() != null) {
            boolean passes = conditionPasses(mechanic.inlineCondition(), context, null);
            if (mechanic.negated() == passes) return false;
        }

        // "cd=N" (Sekunden) sperrt diese Zeile fuer den Caster, egal welche Mechanic sie ist.
        if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;

        // MythicMobs erlaubt "delay" sowohl als eigene Zeile zwischen Mechaniken als auch
        // als Parameter einer einzelnen Mechanic ("command{c=...;delay=70}"): diese eine
        // Mechanic feuert dann isoliert verzoegert, ohne die restlichen Schritte aufzuhalten.
        if (p.containsKey("delay")) {
            int ticks = parseInt(p.get("delay"), 0);
            // cd ebenfalls entfernen: die Kopie hat eine neue Identitaet und wuerde sonst
            // einen zweiten, wirkungslosen Cooldown-Eintrag anlegen.
            SkillStep.Mechanic withoutDelay = new SkillStep.Mechanic(mechanic.name(),
                    without(without(p, "delay"), "cd"), mechanic.targeter(), mechanic.targeterParams(), null, false);
            Tasks.runLater(plugin, context.caster(), ticks, () -> runMechanic(withoutDelay, context));
            return false;
        }

        if (mechanic.name().equals("cancelskill")) return true;

        List<Target> targets = resolveAll(mechanic.targeter(), mechanic.targeterParams(), context);
        // Ein Targeter wie @PIR findet unter Umstaenden niemanden - dann gibt es nichts auszufuehren.
        if (targets.isEmpty()) return false;
        Map<String, String> params = substitute(p, context);
        for (Target target : targets) dispatch(mechanic, context, target, params);
        return false;
    }

    /** Fuehrt eine Mechanic fuer genau ein aufgeloestes Ziel aus (bei Mehrfach-Targetern einmal pro Ziel). */
    private void dispatch(SkillStep.Mechanic mechanic, SkillContext context, Target target, Map<String, String> p) {
        switch (mechanic.name()) {
            case "cancelevent" -> {
                if (context.event() != null) context.event().setCancelled(true);
            }
            case "skill" -> {
                String id = firstParam(p, "s", "skill", "skills");
                if (id == null) break;
                // Mit eigenem Targeter laeuft der Skill fuer dieses Ziel; seine Zeilen ohne Targeter erben es.
                boolean forTarget = !mechanic.targeter().isEmpty() && !mechanic.targeter().equals("self")
                        && target.entity() instanceof LivingEntity;
                SkillContext child = forTarget ? context.withTrigger((LivingEntity) target.entity()) : context;
                if (id.trim().startsWith("[")) runInline(id, child);
                else runById(id.trim(), child);
            }
            case "look" -> look(target, context.caster());
            case "sound" -> sound(target, p);
            case "state" -> state(context.caster(), firstParam(p, "state", "s"));
            case "potion" -> potion(target, p);
            case "breakblock" -> breakBlock(target, p);
            case "gcd" -> setGcd(context.caster().getUniqueId(), p);
            case "model" -> model(target, p);
            case "modelengine" -> modelEngineAttach(target, p);
            case "randomskill" -> randomSkill(context, p);
            case "remove" -> remove(target);
            case "command" -> command(context, p);
            case "summon" -> summon(target, p);
            case "mountmodel" -> mountModel(context, target, p);
            case "effect:particles", "e:p", "particles" -> particles(target, p);
            case "effect:particlering" -> particleRing(target, p);
            case "spin" -> spin(target, p);
            case "takeitem" -> takeItem(target, p);
            case "sudoskill" -> sudoSkill(context, target, p);
            case "damage" -> damage(context, target, p);
            case "throw" -> throwTarget(context, target, p);
            case "lunge" -> lunge(context, target, p);
            case "setblock" -> setBlock(target, p);
            case "equip" -> equip(target, p);
            case "aura", "ondamaged", "onattack", "ontick", "ondeath", "onshoot" -> registerAura(target, p, mechanic.name());
            case "addtag" -> tag(target, p, true);
            case "removetag" -> tag(target, p, false);
            case "bodyrotation" -> bodyRotation(context, p);
            case "ignite" -> ignite(target, p);
            case "shoot" -> shoot(context, p);
            case "stun" -> stun(target, p);
            case "velocity" -> velocity(target, p);
            case "freeze" -> freeze(target, p);
            case "setnodamageticks" -> {
                if (target.entity() instanceof LivingEntity living) living.setNoDamageTicks(parseInt(firstParam(p, "ticks", "t"), 0));
            }
            case "totem" -> totem(context, target, p);
            default -> {
                CustomMechanicEntry custom = customMechanics.get(mechanic.name());
                if (custom == null) {
                    plugin.getLogger().warning("Skill-Mechanic '" + mechanic.name() + "' wird nicht unterstuetzt.");
                } else {
                    try {
                        custom.mechanic().execute(new MechanicContext(context.caster(), context.trigger(), context.event(),
                                target.entity(), target.location(), p));
                    } catch (RuntimeException exception) {
                        plugin.getLogger().warning("Eigene Mechanic '" + mechanic.name() + "' von " + custom.owner().getName()
                                + " ist fehlgeschlagen: " + exception);
                    }
                }
            }
        }
    }

    private static Map<String, String> without(Map<String, String> params, String key) {
        Map<String, String> copy = new LinkedHashMap<>(params);
        copy.remove(key);
        return copy;
    }

    /** Eine Bedingungszeile: "name{params} [true|false] [aktion [wert]]", z.B. "distance{d=0-6} castinstead mein_skill". */
    private record Condition(String name, String params, Boolean expected, String action, String actionValue) {}

    private static Condition parseCondition(String raw) {
        String text = raw.trim();
        int i = 0;
        while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_' || text.charAt(i) == ':')) i++;
        if (i == 0) return null;
        String name = text.substring(0, i).toLowerCase(Locale.ROOT);

        String params = null;
        if (i < text.length() && text.charAt(i) == '{') {
            int depth = 0;
            int j = i;
            for (; j < text.length(); j++) {
                char c = text.charAt(j);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            params = text.substring(i + 1, Math.min(j, text.length()));
            i = j + 1;
        }

        Boolean expected = null;
        String action = null;
        String value = null;
        String rest = i < text.length() ? text.substring(i).trim() : "";
        if (!rest.isEmpty()) {
            String[] tokens = rest.split("\\s+");
            int k = 0;
            if (tokens[0].equalsIgnoreCase("true") || tokens[0].equalsIgnoreCase("false")) {
                expected = Boolean.parseBoolean(tokens[0]);
                k = 1;
            }
            if (k < tokens.length) {
                action = tokens[k].toLowerCase(Locale.ROOT);
                if (k + 1 < tokens.length) value = String.join(" ", java.util.Arrays.copyOfRange(tokens, k + 1, tokens.length));
            }
        }
        return new Condition(name, params, expected, action, value);
    }

    /** Einzelne Bedingung ohne Aktion, z.B. fuer "?cond{...}" am Zeilenende: erfuellt = true. */
    private boolean conditionPasses(String raw, SkillContext context, Target targetOverride) {
        Condition condition = parseCondition(raw);
        if (condition == null) return true;
        return evaluate(condition, context, targetOverride) == (condition.expected() == null || condition.expected());
    }

    private boolean evaluate(Condition condition, SkillContext context, Target targetOverride) {
        String paramsRaw = condition.params();
        return switch (condition.name()) {
            case "offgcd" -> !hasActiveGcd(context.caster().getUniqueId());
            case "onground" -> context.caster().isOnGround();
            case "hasaura" -> hasAura(context.caster(), conditionParam(paramsRaw, "n", "name", "aura", "auraname"));
            case "hastag" -> context.caster().getScoreboardTags().contains(TAG_PREFIX + conditionParam(paramsRaw, "t", "tag", "n"));
            case "chance" -> ThreadLocalRandom.current().nextDouble() < parseFloat(conditionParam(paramsRaw, "chance", "c"), 1f);
            case "skilloncooldown" -> skillOnCooldown(context.caster(), conditionParam(paramsRaw, "skill", "s", "name"));
            case "faction" -> hasFaction(context.caster(), conditionParam(paramsRaw, "faction", "f", "name"));
            case "distance" -> withinDistance(context, conditionParam(paramsRaw, "d", "distance"));
            case "onblock" -> containsBlockType(paramsRaw, context.caster().getLocation().subtract(0, 0.1, 0).getBlock());
            case "blocktype" -> targetOverride != null && targetOverride.block() != null
                    && containsBlockType(paramsRaw, targetOverride.block());
            default -> {
                plugin.getLogger().warning("Skill-Condition '" + condition.name() + "' wird nicht unterstuetzt - wird ignoriert.");
                yield true;
            }
        };
    }

    private boolean hasFaction(Entity entity, String names) {
        String faction = mobManager.factionOf(entity);
        if (faction == null || names == null) return false;
        for (String name : names.split(",")) {
            if (faction.equals(name.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    /** Abstand zum Ausloeser (oder zum Ziel des Mobs): "0-6" Bereich, ">3", "<5", ">=2", "<=4" oder ein Wert (+-0,5). */
    private boolean withinDistance(SkillContext context, String spec) {
        LivingEntity other = context.trigger();
        if (other == null && context.caster() instanceof Mob mob) other = mob.getTarget();
        if (other == null || !other.getWorld().equals(context.caster().getWorld())) return false;
        double distance = other.getLocation().distance(context.caster().getLocation());
        try {
            if (spec.startsWith(">=")) return distance >= Double.parseDouble(spec.substring(2));
            if (spec.startsWith("<=")) return distance <= Double.parseDouble(spec.substring(2));
            if (spec.startsWith(">")) return distance > Double.parseDouble(spec.substring(1));
            if (spec.startsWith("<")) return distance < Double.parseDouble(spec.substring(1));
            int dash = spec.indexOf('-', 1);
            if (dash > 0) return distance >= Double.parseDouble(spec.substring(0, dash)) && distance <= Double.parseDouble(spec.substring(dash + 1));
            return Math.abs(distance - Double.parseDouble(spec)) < 0.5;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean containsBlockType(String paramsRaw, Block block) {
        if (paramsRaw == null) return false;
        int eq = paramsRaw.indexOf('=');
        String list = eq < 0 ? paramsRaw : paramsRaw.substring(eq + 1);
        for (String type : list.split(",")) {
            if (type.trim().equalsIgnoreCase(block.getType().name())) return true;
        }
        return false;
    }

    private void look(Target target, LivingEntity caster) {
        Location from = caster.getLocation();
        Location facing = from.clone();
        facing.setDirection(target.location().toVector().subtract(from.toVector()));
        caster.setRotation(facing.getYaw(), facing.getPitch());
    }

    private void sound(Target target, Map<String, String> p) {
        String name = p.get("s");
        if (name == null) return;
        float pitch = parseFloat(p.get("p"), 1f);
        float volume = parseFloat(p.get("v"), 1f);
        Location location = target.location();
        location.getWorld().playSound(location, name, SoundCategory.HOSTILE, volume, pitch);
    }

    /** ponytail: nur PLAY_ONCE, li/lo/speed/n aus der Vorlage werden von BetterModels
     *  AnimationModifier hier nicht ausgewertet - fuer eine Loop-Steuerung Modifier erweitern. */
    private void state(LivingEntity caster, String stateName) {
        if (stateName == null) return;
        betterModel.play(mobManager.trackerFor(caster.getUniqueId()), stateName);
    }

    private void potion(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity living)) return;
        PotionEffectType type = PotionEffectType.getByName(firstParam(p, "type", "t") == null ? "SLOW" : firstParam(p, "type", "t").trim());
        if (type == null) {
            plugin.getLogger().warning("Unbekannter Potion-Typ '" + p.get("type") + "'.");
            return;
        }
        int duration = parseInt(firstParam(p, "duration", "d"), 20);
        int level = parseInt(firstParam(p, "level", "l"), 1);
        living.addPotionEffect(new PotionEffect(type, duration, Math.max(0, level - 1)));
    }

    private void breakBlock(Target target, Map<String, String> p) {
        if (target.block() == null) return;
        boolean useTool = Boolean.parseBoolean(p.getOrDefault("usetool", "false"));
        if (useTool) target.block().breakNaturally();
        else target.block().setType(Material.AIR);
    }

    /** ponytail: "n" (Modell-Name/-Slot aus der Vorlage) wird nicht ausgewertet - wir
     *  haben pro Entity nur einen BetterModel-Tracker, kein benanntes Multi-Model-Setup. */
    private void model(Target target, Map<String, String> p) {
        String modelId = p.get("mid");
        if (modelId == null || target.entity() == null) return;
        Object tracker = betterModel.attach(target.entity(), modelId);
        mobManager.replaceTracker(target.entity(), tracker);
    }

    /** Pendant zu "model", haengt das Modell aber ueber ModelEngine statt BetterModel an -
     *  fuer Packs, deren Modelle in ModelEngine statt BetterModel registriert sind. */
    private void modelEngineAttach(Target target, Map<String, String> p) {
        String modelId = p.get("mid");
        if (modelId == null || target.entity() == null) return;
        Object tracker = modelEngine.attach(target.entity(), modelId);
        mobManager.replaceModelEngineTracker(target.entity(), tracker);
    }

    /** "s=a,b,c" (auch "skills=...", ueber mehrere YAML-Zeilen) waehlt zufaellig einen der
     *  Skills. "sync" wird ignoriert - wir sind ohnehin immer auf dem Main-Thread unterwegs. */
    private void randomSkill(SkillContext context, Map<String, String> p) {
        String list = firstParam(p, "s", "skills", "skill");
        if (list == null) return;
        String[] ids = list.split(",");
        if (ids.length == 0) return;
        runById(ids[ThreadLocalRandom.current().nextInt(ids.length)].trim(), context);
    }

    private boolean acquireCooldown(SkillContext context, SkillStep.Mechanic mechanic) {
        float seconds = parseFloat(mechanic.params().get("cd"), 0f);
        if (seconds <= 0) return true;
        long now = System.currentTimeMillis();
        String key = context.caster().getUniqueId() + "#" + System.identityHashCode(mechanic);
        Long until = cooldowns.get(key);
        if (until != null && until > now) return false;
        if (cooldowns.size() > 2048) cooldowns.values().removeIf(time -> time <= now);
        cooldowns.put(key, now + (long) (seconds * 1000));
        return true;
    }

    private static String firstParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String value = params.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private void particles(Target target, Map<String, String> p) {
        Particle particle = particle(firstParam(p, "p", "particle"));
        if (particle == null) return;
        Location at = target.location().clone().add(0, parseFloat(firstParam(p, "y", "yoffset"), 0f), 0);
        spawnParticles(at, particle, p);
        // repeat=N;repeatInterval=T: der Schub wird N weitere Male alle T Ticks wiederholt.
        int repeat = parseInt(p.get("repeat"), 0);
        long interval = Math.max(1, parseInt(p.get("repeatinterval"), 1));
        Entity anchor = target.entity() != null ? target.entity() : null;
        for (int i = 1; i <= repeat && anchor != null; i++) {
            Tasks.runLater(plugin, anchor, i * interval, () -> spawnParticles(target.location().clone().add(0, at.getY() - target.location().getY(), 0), particle, p));
        }
    }

    /** Ring aus "points" Punkten mit "radius" um das Ziel, an jedem Punkt ein Partikelschub. */
    private void particleRing(Target target, Map<String, String> p) {
        Particle particle = particle(firstParam(p, "particle", "p"));
        if (particle == null) return;
        double radius = parseFloat(p.get("radius"), 1f);
        int points = Math.max(1, parseInt(p.get("points"), 8));
        Location center = target.location();
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            spawnParticles(center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), particle, p);
        }
    }

    private void spawnParticles(Location location, Particle particle, Map<String, String> p) {
        int amount = Math.max(1, parseInt(firstParam(p, "amount", "a"), 1));
        double horizontal = parseFloat(p.get("hs"), 0f);
        double vertical = parseFloat(p.get("vs"), 0f);
        double speed = parseFloat(firstParam(p, "speed", "s"), 0f);
        try {
            location.getWorld().spawnParticle(particle, location, amount, horizontal, vertical, horizontal, speed);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Partikel '" + particle + "' braucht Zusatzdaten (z.B. Farbe) und wird nicht unterstuetzt.");
        }
    }

    private Particle particle(String name) {
        if (name == null) return null;
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unbekannter Partikel '" + name + "'.");
            return null;
        }
    }

    /** Dreht das Ziel "duration" Ticks lang um "velocity" Grad pro Tick (z.B. die Karte beim Aufdecken). */
    private void spin(Target target, Map<String, String> p) {
        Entity entity = target.entity();
        if (entity == null) return;
        int duration = parseInt(p.get("duration"), 100);
        float velocity = parseFloat(p.get("velocity"), 10f);
        int[] elapsed = {0};
        Runnable[] cancel = new Runnable[1];
        cancel[0] = Tasks.runTimer(plugin, entity, 1L, 1L, () -> {
            if (++elapsed[0] > duration || !entity.isValid()) {
                cancel[0].run();
                return;
            }
            entity.setRotation(entity.getYaw() + velocity, entity.getPitch());
        });
    }

    /** Nimmt "a" Stueck des BetterMob-Items "i" aus dem Inventar des Ziel-Spielers (z.B. das Pack beim Oeffnen). */
    private void takeItem(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof Player player)) return;
        String itemId = firstParam(p, "i", "item", "type");
        if (itemId == null) return;
        int remaining = Math.max(1, parseInt(firstParam(p, "a", "amount"), 1));

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !itemId.trim().equalsIgnoreCase(items.idOf(stack))) continue;
            int taken = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - taken);
            player.getInventory().setItem(slot, stack.getAmount() <= 0 ? null : stack);
            remaining -= taken;
        }
    }

    /** Fuehrt den Skill "s" mit dem Ziel als Caster aus (der Spieler "wirkt" ihn selbst); der urspruengliche Caster wird Trigger. */
    private void sudoSkill(SkillContext context, Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity executor)) return;
        String id = firstParam(p, "s", "skill", "skills");
        if (id != null) runById(id.trim(), new SkillContext(executor, context.caster(), null));
    }

    boolean isApplyingDamage() {
        return applyingDamage.get();
    }

    /** "amount" Schaden am Ziel, mit dem Caster als Verursacher (also mit Rüstung, Cooldowns und Events wie ein normaler Treffer). */
    private void damage(SkillContext context, Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity victim)) return;
        double amount = parseFloat(firstParam(p, "amount", "a"), 1f);
        applyingDamage.set(true);
        try {
            victim.damage(amount, context.caster());
        } finally {
            applyingDamage.set(false);
        }
    }

    /**
     * Schleudert das Ziel vom Caster weg. ponytail: "velocity" wird durch 10 geteilt (so staerken
     * sich velocity=8 beim Wurf und velocity=0.8 beim Lunge im Pack), "velocityY" ist direkt
     * Bloecke pro Tick nach oben - die genaue MythicMobs-Skalierung ist hier geschaetzt.
     */
    private void throwTarget(SkillContext context, Target target, Map<String, String> p) {
        Entity thrown = target.entity();
        if (thrown == null || thrown instanceof LivingEntity living && living.isDead()) return;
        Vector away = thrown.getLocation().toVector().subtract(context.caster().getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 1e-6) away = context.caster().getLocation().getDirection().setY(0);
        thrown.setVelocity(away.normalize().multiply(parseFloat(firstParam(p, "velocity", "v"), 4f) / 10.0)
                .setY(parseFloat(firstParam(p, "velocityy", "vy"), 0f) / 10.0));
    }

    /** Der Caster springt mit "velocity" Bloecken pro Tick auf das Ziel zu. */
    private void lunge(SkillContext context, Target target, Map<String, String> p) {
        Vector toward = target.location().toVector().subtract(context.caster().getLocation().toVector()).setY(0);
        if (toward.lengthSquared() < 1e-6) return;
        context.caster().setVelocity(toward.normalize().multiply(parseFloat(p.get("velocity"), 1f))
                .setY(parseFloat(firstParam(p, "velocityy", "vy"), 0f)));
    }

    /** Setzt den Block am Ziel auf "m" (z.B. Gras -> Erde beim Grasen). */
    private void setBlock(Target target, Map<String, String> p) {
        Material material = Material.matchMaterial(firstParam(p, "m", "material", "type", "block") == null ? "" : firstParam(p, "m", "material", "type", "block").trim());
        if (material == null || !material.isBlock()) {
            plugin.getLogger().warning("setblock: unbekanntes Material '" + firstParam(p, "m", "material", "type", "block") + "'.");
            return;
        }
        target.location().getBlock().setType(material);
    }

    /**
     * "item=<item>:<slot>" legt dem Ziel einen Gegenstand an. Item ist ein registriertes BetterMob-Item
     * oder ein Vanilla-Material, Slot HAND (Standard), OFFHAND, HEAD, CHEST, LEGS oder FEET.
     */
    private void equip(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity living) || living.getEquipment() == null) return;
        String spec = firstParam(p, "item", "i", "type");
        if (spec == null) return;
        String[] parts = spec.trim().split(":", 2);

        ItemStack stack;
        ItemDefinition custom = items.get(parts[0]);
        if (custom != null) {
            stack = items.create(custom, 1);
        } else {
            Material material = Material.matchMaterial(parts[0]);
            if (material == null || !material.isItem()) {
                plugin.getLogger().warning("equip: '" + parts[0] + "' ist weder ein Item noch ein Material.");
                return;
            }
            stack = new ItemStack(material);
        }

        EquipmentSlot slot = switch ((parts.length > 1 ? parts[1] : "hand").trim().toLowerCase(Locale.ROOT)) {
            case "offhand", "off_hand" -> EquipmentSlot.OFF_HAND;
            case "head", "helmet" -> EquipmentSlot.HEAD;
            case "chest", "chestplate" -> EquipmentSlot.CHEST;
            case "legs", "leggings" -> EquipmentSlot.LEGS;
            case "feet", "boots" -> EquipmentSlot.FEET;
            default -> EquipmentSlot.HAND;
        };
        EntityEquipment equipment = living.getEquipment();
        equipment.setItem(slot, stack);
        // Gegenstaende, die ein Skill anlegt, sollen beim Tod nicht zusaetzlich herumliegen.
        if (living instanceof Mob mob) {
            equipment.setDropChance(slot, 0f);
            MobDefinition definition = mobManager.definitionOf(mob.getUniqueId());
            if (definition != null && !definition.aiGoalSelectors.isEmpty()) AiGoalApplier.promoteRanged(mob);
        }
    }

    private static final String TAG_PREFIX = "bettermob_tag_";

    /** Liest einen Parameter aus dem rohen "{a=b;c=d}"-Inhalt einer Bedingung. */
    private static String conditionParam(String paramsRaw, String... keys) {
        if (paramsRaw == null) return "";
        String value = firstParam(SkillStep.parseParams(paramsRaw), keys);
        return value == null ? "" : value.trim();
    }

    private static final class Aura {
        final String kind;
        final long until;
        final boolean cancelEvent;
        final String onEnd;
        final String onHit;
        Runnable cancelTicker = () -> { };
        Runnable cancelEnd = () -> { };

        Aura(String kind, long until, boolean cancelEvent, String onEnd, String onHit) {
            this.kind = kind;
            this.until = until;
            this.cancelEvent = cancelEvent;
            this.onEnd = onEnd;
            this.onHit = onHit;
        }

        void stop() {
            cancelTicker.run();
            cancelEnd.run();
        }
    }

    private void registerAura(Target target, Map<String, String> p, String kind) {
        String name = firstParam(p, "auraname", "name", "aura");
        if (name == null || !(target.entity() instanceof LivingEntity entity)) return;
        String key = name.toLowerCase(Locale.ROOT);
        int ticks = parseInt(firstParam(p, "time", "ticks", "duration"), 0);
        long until = ticks > 0 ? System.currentTimeMillis() + ticks * 50L : Long.MAX_VALUE;
        Aura aura = new Aura(kind, until, Boolean.parseBoolean(firstParam(p, "ce", "cancelevent")),
                firstParam(p, "oe", "onend"), firstParam(p, "oh", "onhit"));

        Map<String, Aura> active = auras.computeIfAbsent(entity.getUniqueId(), id -> new ConcurrentHashMap<>());
        Aura old = active.put(key, aura);
        if (old != null) old.stop();

        runAuraLines(firstParam(p, "os", "onstart"), SkillContext.of(entity));

        String onTick = firstParam(p, "ot", "ontick");
        if (onTick != null) {
            long interval = Math.max(1, parseInt(firstParam(p, "i", "interval"), 20));
            aura.cancelTicker = Tasks.runTimer(plugin, entity, interval, interval, () -> {
                if (entity.isDead() || active.get(key) != aura) aura.cancelTicker.run();
                else runAuraLines(onTick, SkillContext.of(entity));
            });
        }
        if (ticks > 0) {
            Tasks.runLater(plugin, entity, ticks, () -> endAura(entity, active, key, aura));
        }
    }

    private void endAura(LivingEntity entity, Map<String, Aura> active, String key, Aura aura) {
        if (!active.remove(key, aura)) return;
        aura.stop();
        runAuraLines(aura.onEnd, SkillContext.of(entity));
    }

    private void runAuraLines(String lines, SkillContext context) {
        if (lines != null) executeSteps(inlineSkills.computeIfAbsent(lines, this::parseInline), 0, context);
    }

    void fireAuras(LivingEntity entity, String kind, LivingEntity trigger, org.bukkit.event.Cancellable event) {
        Map<String, Aura> active = auras.get(entity.getUniqueId());
        if (active == null) return;
        for (Aura aura : active.values()) {
            if (!aura.kind.equals(kind) || aura.until <= System.currentTimeMillis()) continue;
            if (aura.cancelEvent && event != null) event.setCancelled(true);
            runAuraLines(aura.onHit, new SkillContext(entity, trigger, event));
        }
    }

    private boolean hasAura(LivingEntity entity, String name) {
        Map<String, Aura> active = auras.get(entity.getUniqueId());
        if (active == null) return false;
        Aura aura = active.get(name.toLowerCase(Locale.ROOT));
        if (aura == null) return false;
        if (aura.until <= System.currentTimeMillis()) {
            active.remove(name.toLowerCase(Locale.ROOT), aura);
            return false;
        }
        return true;
    }

    /** addtag{t=...}/removetag{t=...}: Markierungen am Ziel, abfragbar mit ?hastag{t=...}. */
    private void tag(Target target, Map<String, String> p, boolean add) {
        String tag = firstParam(p, "t", "tag");
        if (tag == null || target.entity() == null) return;
        if (add) target.entity().addScoreboardTag(TAG_PREFIX + tag.trim());
        else target.entity().removeScoreboardTag(TAG_PREFIX + tag.trim());
    }

    /** Vergisst den Zustand eines entfernten Entities (Auren, globaler Cooldown). */
    void forget(UUID entityId) {
        Map<String, Aura> removed = auras.remove(entityId);
        if (removed != null) removed.values().forEach(Aura::stop);
        gcdUntilMillis.remove(entityId);
    }

    /** Skill-Zeilen direkt im Parameter: "skill{s=[ - sound{...} - delay 5 ]}" - einmal geparst und gemerkt. */
    private void runInline(String raw, SkillContext context) {
        executeSteps(inlineSkills.computeIfAbsent(raw, this::parseInline), 0, context);
    }

    private List<SkillStep> parseInline(String raw) {
        List<SkillStep> steps = new ArrayList<>();
        for (String line : splitInline(raw)) {
            SkillStep step = SkillStep.parse(line);
            if (step == null) plugin.getLogger().warning("Inline-Skill: Zeile '" + line + "' konnte nicht geparst werden.");
            else steps.add(step);
        }
        return List.copyOf(steps);
    }

    /** "[ - a{..} - b{..} ]" in die einzelnen Zeilen zerlegen: an jedem "-" auf oberster Ebene, das von Leerraum umgeben ist. */
    private static List<String> splitInline(String raw) {
        String body = raw.trim();
        if (body.startsWith("[")) body = body.substring(1);
        if (body.endsWith("]")) body = body.substring(0, body.length() - 1);

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            boolean separator = c == '-' && depth == 0 && (i == 0 || Character.isWhitespace(body.charAt(i - 1)))
                    && i + 1 < body.length() && Character.isWhitespace(body.charAt(i + 1));
            if (separator) {
                lines.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        lines.add(current.toString().trim());
        lines.removeIf(String::isBlank);
        return lines;
    }

    /** <caster.damage> u.a. in Parameterwerten durch den echten Wert ersetzen (der Angriffsschaden des Casters, sein Name). */
    private Map<String, String> substitute(Map<String, String> p, SkillContext context) {
        boolean placeholders = false;
        for (String value : p.values()) {
            if (value.indexOf("<caster.") >= 0) {
                placeholders = true;
                break;
            }
        }
        if (!placeholders) return p;
        var attackDamage = context.caster().getAttribute(Attribute.ATTACK_DAMAGE);
        String damage = String.valueOf(attackDamage == null ? 1.0 : attackDamage.getValue());
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : p.entrySet()) {
            result.put(entry.getKey(), entry.getValue()
                    .replace("<caster.damage>", damage).replace("<caster.name>", context.caster().getName()));
        }
        return result;
    }

    private void bodyRotation(SkillContext context, Map<String, String> p) {
        UUID id = context.caster().getUniqueId();
        if (betterModel.bodyRotation(mobManager.trackerFor(id), p)) return;
        java.util.concurrent.atomic.AtomicBoolean applied = new java.util.concurrent.atomic.AtomicBoolean();
        for (int attempt = 1; attempt <= 5; attempt++) {
            Tasks.runLater(plugin, context.caster(), attempt * 2L, () -> {
                Object tracker = mobManager.trackerFor(id);
                if (tracker != null && !applied.get() && betterModel.bodyRotation(tracker, p)) applied.set(true);
            });
        }
    }

    private void ignite(Target target, Map<String, String> p) {
        if (target.entity() != null) target.entity().setFireTicks(parseInt(firstParam(p, "t", "ticks", "duration", "d"), 100));
    }

    private void totem(SkillContext context, Target target, Map<String, String> p) {
        Location origin = target.location().clone().add(0, parseFloat(firstParam(p, "yo", "yoffset"), 0f), 0);
        SkillContext at = context.withOrigin(origin);
        runTotemLines(firstParam(p, "os", "onstart"), at);

        int duration = parseInt(firstParam(p, "md", "maxduration"), 0);
        String onTick = firstParam(p, "ot", "ontick");
        String onEnd = firstParam(p, "oe", "onend");
        if (duration <= 0 || (onTick == null && onEnd == null)) return;

        long interval = Math.max(1, parseInt(firstParam(p, "i", "interval"), 20));
        long[] elapsed = {0};
        Runnable[] cancel = {() -> { }};
        cancel[0] = Tasks.runTimer(plugin, context.caster(), interval, interval, () -> {
            if (context.caster().isDead()) {
                cancel[0].run();
                return;
            }
            elapsed[0] += interval;
            runTotemLines(onTick, at);
            if (elapsed[0] >= duration) {
                cancel[0].run();
                runTotemLines(onEnd, at);
            }
        });
    }

    private void runTotemLines(String lines, SkillContext context) {
        if (lines != null) executeSteps(inlineSkills.computeIfAbsent(lines, this::parseInline), 0, context);
    }

    private void remove(Target target) {
        if (target.entity() != null) target.entity().remove();
    }

    /** "c" ist der Command-String, <caster.name>/<target.name> werden ersetzt. Laeuft
     *  immer als Konsolenbefehl - MythicMobs' "AsOp/AsCaster"-Unterscheidung gibt es hier nicht. */
    private void command(SkillContext context, Map<String, String> p) {
        String raw = p.get("c");
        if (raw == null) return;
        String command = stripQuotes(raw)
                .replace("<caster.name>", context.caster().getName())
                .replace("<target.name>", context.trigger() != null ? context.trigger().getName() : context.caster().getName());
        Tasks.runGlobal(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }

    /** "type" muss die ID eines in mobs/ registrierten Mobs sein - Vanilla-EntityTypes
     *  direkt summonen unterstuetzen wir nicht, dafuer gibt es keinen Anwendungsfall im Pack. */
    private void summon(Target target, Map<String, String> p) {
        String mobId = firstParam(p, "type", "t", "mob", "m");
        if (mobId == null) return;
        MobDefinition definition = mobManager.registry().get(mobId.trim());
        if (definition == null) {
            plugin.getLogger().warning("summon: Mob '" + mobId + "' ist nicht registriert.");
            return;
        }
        mobManager.spawn(definition, target.location());
    }

    /** Setzt den Zieltrigger (z.B. den klickenden Spieler) auf einen benannten Sitz des
     *  Caster-Modells - BetterModel uebernimmt danach Lenkung/Fahrverhalten selbst. */
    private void mountModel(SkillContext context, Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity rider)) return;
        Object tracker = mobManager.trackerFor(context.caster().getUniqueId());
        if (tracker == null) {
            plugin.getLogger().warning("mountmodel: kein BetterModel-Tracker am Caster vorhanden.");
            return;
        }
        betterModel.mount(tracker, p.getOrDefault("seat", "mount"), rider);
    }

    private void setGcd(UUID casterId, Map<String, String> p) {
        int ticks = parseInt(p.get("ticks"), 20);
        gcdUntilMillis.put(casterId, System.currentTimeMillis() + ticks * 50L);
    }

    private boolean hasActiveGcd(UUID casterId) {
        Long until = gcdUntilMillis.get(casterId);
        return until != null && until > System.currentTimeMillis();
    }

    private Target resolve(String targeter, Map<String, String> targeterParams, SkillContext context) {
        return switch (targeter.toLowerCase(Locale.ROOT)) {
            case "trigger", "target" -> context.trigger() != null ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster());
            case "obstructingblock" -> Target.ofBlock(obstructingBlock(context.caster()));
            case "forward" -> Target.ofLocation(forwardLocation(context.caster(), targeterParams));
            case "selflocation" -> Target.ofLocation(context.caster().getLocation().add(
                    parseFloat(targeterParams.get("x"), 0f), parseFloat(targeterParams.get("y"), 0f), parseFloat(targeterParams.get("z"), 0f)));
            // Bone-Position des Modells (p=tnt2); fehlt der Bone oder das Modell, die Brusthoehe des Casters.
            case "modelpart" -> {
                Location bone = betterModel.bonePosition(mobManager.trackerFor(context.caster().getUniqueId()),
                        firstParam(targeterParams, "p", "part", "bone"), context.caster().getLocation());
                yield Target.ofLocation(bone != null ? bone
                        : context.caster().getLocation().add(0, context.caster().getHeight() * 0.6, 0));
            }
            case "pir", "playersinradius" -> nearestPlayer(context.caster(), targeterParams);
            case "self" -> Target.ofEntity(context.caster());
            // Kein @Targeter geschrieben: das Ziel erben, das der aufrufende Skill gesetzt hat.
            default -> context.targetIsTrigger() && context.trigger() != null
                    ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster());
        };
    }

    /** Wie resolve(), aber Mehrfach-Targeter (@EntitiesNearOrigin, @EntitiesInRadius) liefern mehrere Ziele. */
    private List<Target> resolveAll(String targeter, Map<String, String> params, SkillContext context) {
        String key = targeter.toLowerCase(Locale.ROOT);
        boolean nearOrigin = key.equals("entitiesnearorigin") || key.equals("eno");
        if (nearOrigin || key.equals("entitiesinradius") || key.equals("eir")) {
            Location center = nearOrigin && context.origin() != null ? context.origin() : context.caster().getLocation();
            return entitiesInRadius(center, params, context);
        }
        Target single = resolve(targeter, params, context);
        return single == null ? List.of() : List.of(single);
    }

    /** Lebewesen im Radius "r" um center, nach Abstand sortiert; "conditions=[ - isPlayer{} true ... ]" filtert. */
    private List<Target> entitiesInRadius(Location center, Map<String, String> params, SkillContext context) {
        double radius = parseFloat(params.get("r"), 5f);
        List<String> conditions = params.containsKey("conditions") ? splitInline(params.get("conditions")) : List.of();
        List<LivingEntity> hits = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand || living.isDead()) continue;
            if (living.getLocation().distanceSquared(center) > radius * radius) continue;
            if (candidateMatches(living, conditions, context)) hits.add(living);
        }
        hits.sort(java.util.Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)));
        List<Target> targets = new ArrayList<>();
        for (LivingEntity hit : hits) targets.add(Target.ofEntity(hit));
        return targets;
    }

    /** Bedingungen eines Targeters pruefen ein einzelnes Kandidaten-Lebewesen: isPlayer, isCaster. */
    private boolean candidateMatches(LivingEntity candidate, List<String> conditions, SkillContext context) {
        for (String raw : conditions) {
            Condition condition = parseCondition(raw);
            if (condition == null) continue;
            boolean actual = switch (condition.name()) {
                case "isplayer" -> candidate instanceof Player;
                case "iscaster" -> candidate.equals(context.caster());
                case "faction" -> hasFaction(candidate, conditionParam(condition.params(), "faction", "f", "name"));
                default -> {
                    plugin.getLogger().warning("Targeter-Condition '" + condition.name() + "' wird nicht unterstuetzt - wird ignoriert.");
                    yield true;
                }
            };
            if (actual != (condition.expected() == null || condition.expected())) return false;
        }
        return true;
    }

    /** ponytail: nur der naechste Spieler im Radius "r" - sort/limit aus @PIR{...} werden nicht ausgewertet, im Pack ist es immer NEAREST/1. */
    private Target nearestPlayer(LivingEntity caster, Map<String, String> p) {
        double radius = parseFloat(p.get("r"), 10f);
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player player)) continue;
            double distance = player.getLocation().distanceSquared(caster.getLocation());
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest == null ? null : Target.ofEntity(nearest);
    }

    /** "f" = Abstand nach vorne, "uel" = ab Augenhoehe statt Fussposition starten, "yoffset" = vertikale
     *  Verschiebung danach, "rotate" = Grad, um die die Blickrichtung seitlich gedreht wird (positiv = rechts). */
    private Location forwardLocation(LivingEntity caster, Map<String, String> p) {
        Location origin = Boolean.parseBoolean(p.getOrDefault("uel", "false")) ? caster.getEyeLocation() : caster.getLocation();
        double distance = parseFloat(p.get("f"), 1f);
        double yOffset = parseFloat(p.get("yoffset"), 0f);
        double rotate = parseFloat(p.get("rotate"), 0f);
        Vector direction = origin.getDirection().normalize();
        // Bukkits rotateAroundY dreht bei positivem Winkel nach links, MythicMobs' rotate nach rechts.
        if (rotate != 0) direction.rotateAroundY(Math.toRadians(-rotate));
        Location target = origin.clone().add(direction.multiply(distance));
        target.add(0, yOffset, 0);
        return target;
    }

    /** Erster nicht-durchsichtbare Block in Blickrichtung des Mobs. */
    private Block obstructingBlock(LivingEntity caster) {
        var result = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), caster.getEyeLocation().getDirection(), 2.5);
        return result != null ? result.getHitBlock() : caster.getEyeLocation().add(caster.getEyeLocation().getDirection()).getBlock();
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return value == null ? fallback : Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record Target(Entity entity, Block block, Location rawLocation) {
        static Target ofEntity(Entity entity) {
            return new Target(entity, null, null);
        }

        static Target ofBlock(Block block) {
            return new Target(null, block, null);
        }

        static Target ofLocation(Location location) {
            return new Target(null, null, location);
        }

        Location location() {
            if (entity != null) return entity.getLocation();
            if (block != null) return block.getLocation().add(0.5, 0.5, 0.5);
            return rawLocation;
        }
    }
}

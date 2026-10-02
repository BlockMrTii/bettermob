package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.ai.AiGoalApplier;
import eu.northsoft.bettermob.api.CustomMechanic;
import eu.northsoft.bettermob.api.MechanicContext;
import eu.northsoft.bettermob.item.ItemDefinition;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
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

public final class SkillEngine implements org.bukkit.event.Listener {
    private final BetterMobPlugin plugin;
    private final SkillRegistry registry;
    private final MobManager mobManager;
    private final BetterModelHook betterModel;
    private final ModelEngineHook modelEngine;
    private final ItemRegistry items;
    private final Map<UUID, Long> gcdUntilMillis = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    private final Map<UUID, Map<String, Aura>> auras = new ConcurrentHashMap<>();
    private final Map<String, List<SkillStep>> inlineSkills = new ConcurrentHashMap<>();

    private final ThreadLocal<Boolean> applyingDamage = ThreadLocal.withInitial(() -> false);
    private final Map<String, CustomMechanicEntry> customMechanics = new ConcurrentHashMap<>();

    private static final Set<String> BUILTIN_MECHANICS = Set.of("cancelskill", "cancelevent", "skill", "look", "sound",
            "state", "potion", "breakblock", "gcd", "model", "modelengine", "randomskill", "remove", "command",
            "summon", "mountmodel", "delay", "effect:particles", "e:p", "particles", "effect:particlering", "spin", "takeitem", "sudoskill", "damage", "throw", "lunge", "setblock", "equip", "aura", "ondamaged", "onattack", "ontick", "ondeath",
            "onshoot", "bodyrotation", "addtag", "removetag", "ignite", "totem", "velocity", "freeze", "shoot", "stun", "setnodamageticks");

    private record CustomMechanicEntry(Plugin owner, CustomMechanic mechanic) {}

    public SkillEngine(BetterMobPlugin plugin, SkillRegistry registry, MobManager mobManager, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.mobManager = mobManager;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @org.bukkit.event.EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setVelocity(new Vector());
        Tasks.runLater(plugin, player, 1L, () -> player.setVelocity(new Vector()));
    }

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

    private void stun(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof Mob mob)) return;
        int ticks = parseInt(firstParam(p, "d", "duration", "t"), 20);
        boolean ai = !"false".equalsIgnoreCase(p.get("ai"));
        boolean gravity = "true".equalsIgnoreCase(p.get("g"));
        boolean freeze = "true".equalsIgnoreCase(p.get("f"));

        String animation = firstParam(p, "state", "animation", "s");
        if (animation != null) state(mob, animation);

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

    public boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic) {
        String key = name.toLowerCase(Locale.ROOT);
        if (BUILTIN_MECHANICS.contains(key)) return false;
        return customMechanics.putIfAbsent(key, new CustomMechanicEntry(owner, mechanic)) == null;
    }

    public void unregisterMechanic(String name) {
        customMechanics.remove(name.toLowerCase(Locale.ROOT));
    }

    public void unregisterMechanics(Plugin owner) {
        customMechanics.values().removeIf(entry -> entry.owner().equals(owner));
    }

    public void runById(String skillId, SkillContext context) {
        SkillDefinition skill = registry.get(skillId);
        if (skill == null) {
            plugin.getLogger().warning("Skill '" + skillId + "' ist nicht registriert.");
            return;
        }
        run(skill, context);
    }

    public void run(SkillDefinition skill, SkillContext context) {
        if (check(skill.conditions, context, null) != Check.PASS) return;
        if (!skill.targetConditions.isEmpty()) {
            Target obstructing = resolve("obstructingblock", Map.of(), context);
            if (check(skill.targetConditions, context, obstructing) != Check.PASS) return;
        }
        if (skill.cooldown > 0 && !acquireSkillCooldown(context.caster(), skill)) return;
        executeSteps(skill.steps, 0, context);
    }

    private enum Check { PASS, FAIL, REDIRECTED }

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

    public void runStep(SkillStep step, SkillContext context) {
        executeSteps(List.of(step), 0, context);
    }

    private void executeSteps(List<SkillStep> steps, int index, SkillContext context) {
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

    private boolean runMechanic(SkillStep.Mechanic mechanic, SkillContext context) {
        Map<String, String> p = mechanic.params();

        if (mechanic.inlineCondition() != null) {
            boolean passes = conditionPasses(mechanic.inlineCondition(), context, null);
            if (mechanic.negated() == passes) return false;
        }

        if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;

        if (p.containsKey("delay")) {
            int ticks = parseInt(p.get("delay"), 0);

            SkillStep.Mechanic withoutDelay = new SkillStep.Mechanic(mechanic.name(),
                    without(without(p, "delay"), "cd"), mechanic.targeter(), mechanic.targeterParams(), null, false);
            Tasks.runLater(plugin, context.caster(), ticks, () -> runMechanic(withoutDelay, context));
            return false;
        }

        if (mechanic.name().equals("cancelskill")) return true;

        List<Target> targets = resolveAll(mechanic.targeter(), mechanic.targeterParams(), context);

        if (targets.isEmpty()) return false;
        Map<String, String> params = substitute(p, context);
        for (Target target : targets) dispatch(mechanic, context, target, params);
        return false;
    }

    private void dispatch(SkillStep.Mechanic mechanic, SkillContext context, Target target, Map<String, String> p) {
        switch (mechanic.name()) {
            case "cancelevent" -> {
                if (context.event() != null) context.event().setCancelled(true);
            }
            case "skill" -> {
                String id = firstParam(p, "s", "skill", "skills");
                if (id == null) break;

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
        if (names == null) return false;
        for (String name : names.split(",")) {
            if (mobManager.inFaction(entity, name.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

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

    private void model(Target target, Map<String, String> p) {
        String modelId = p.get("mid");
        if (modelId == null || target.entity() == null) return;
        Object tracker = betterModel.attach(target.entity(), modelId);
        mobManager.replaceTracker(target.entity(), tracker);
    }

    private void modelEngineAttach(Target target, Map<String, String> p) {
        String modelId = p.get("mid");
        if (modelId == null || target.entity() == null) return;
        Object tracker = modelEngine.attach(target.entity(), modelId);
        mobManager.replaceModelEngineTracker(target.entity(), tracker);
    }

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

        int repeat = parseInt(p.get("repeat"), 0);
        long interval = Math.max(1, parseInt(p.get("repeatinterval"), 1));
        Entity anchor = target.entity() != null ? target.entity() : null;
        for (int i = 1; i <= repeat && anchor != null; i++) {
            Tasks.runLater(plugin, anchor, i * interval, () -> spawnParticles(target.location().clone().add(0, at.getY() - target.location().getY(), 0), particle, p));
        }
    }

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

    private void sudoSkill(SkillContext context, Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity executor)) return;
        String id = firstParam(p, "s", "skill", "skills");
        if (id != null) runById(id.trim(), new SkillContext(executor, context.caster(), null));
    }

    public boolean isApplyingDamage() {
        return applyingDamage.get();
    }

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

    private void throwTarget(SkillContext context, Target target, Map<String, String> p) {
        Entity thrown = target.entity();
        if (thrown == null || thrown instanceof LivingEntity living && living.isDead()) return;
        Vector away = thrown.getLocation().toVector().subtract(context.caster().getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 1e-6) away = context.caster().getLocation().getDirection().setY(0);
        thrown.setVelocity(away.normalize().multiply(parseFloat(firstParam(p, "velocity", "v"), 4f) / 10.0)
                .setY(parseFloat(firstParam(p, "velocityy", "vy"), 0f) / 10.0));
    }

    private void lunge(SkillContext context, Target target, Map<String, String> p) {
        Vector toward = target.location().toVector().subtract(context.caster().getLocation().toVector()).setY(0);
        if (toward.lengthSquared() < 1e-6) return;
        context.caster().setVelocity(toward.normalize().multiply(parseFloat(p.get("velocity"), 1f))
                .setY(parseFloat(firstParam(p, "velocityy", "vy"), 0f)));
    }

    private void setBlock(Target target, Map<String, String> p) {
        Material material = Material.matchMaterial(firstParam(p, "m", "material", "type", "block") == null ? "" : firstParam(p, "m", "material", "type", "block").trim());
        if (material == null || !material.isBlock()) {
            plugin.getLogger().warning("setblock: unbekanntes Material '" + firstParam(p, "m", "material", "type", "block") + "'.");
            return;
        }
        target.location().getBlock().setType(material);
    }

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

        if (living instanceof Mob mob) {
            equipment.setDropChance(slot, 0f);
            MobDefinition definition = mobManager.definitionOf(mob.getUniqueId());
            if (definition != null && !definition.aiGoalSelectors.isEmpty()) AiGoalApplier.promoteRanged(mob);
        }
    }

    private static final String TAG_PREFIX = "bettermob_tag_";

    private static String conditionParam(String paramsRaw, String... keys) {
        if (paramsRaw == null) return "";
        String value = firstParam(SkillStep.parseParams(paramsRaw), keys);
        return value == null ? "" : value.trim();
    }

    private static final class Aura {
        public final String kind;
        public final long until;
        public final boolean cancelEvent;
        public final String onEnd;
        public final String onHit;
        public Runnable cancelTicker = () -> { };
        public Runnable cancelEnd = () -> { };

        public Aura(String kind, long until, boolean cancelEvent, String onEnd, String onHit) {
            this.kind = kind;
            this.until = until;
            this.cancelEvent = cancelEvent;
            this.onEnd = onEnd;
            this.onHit = onHit;
        }

        public void stop() {
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

    public void fireAuras(LivingEntity entity, String kind, LivingEntity trigger, org.bukkit.event.Cancellable event) {
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

    private void tag(Target target, Map<String, String> p, boolean add) {
        String tag = firstParam(p, "t", "tag");
        if (tag == null || target.entity() == null) return;
        if (add) target.entity().addScoreboardTag(TAG_PREFIX + tag.trim());
        else target.entity().removeScoreboardTag(TAG_PREFIX + tag.trim());
    }

    public void forget(UUID entityId) {
        Map<String, Aura> removed = auras.remove(entityId);
        if (removed != null) removed.values().forEach(Aura::stop);
        gcdUntilMillis.remove(entityId);
    }

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
        String onHit = firstParam(p, "oh", "onhit");
        if (onHit != null) spawnTotemBody(context, at, onHit, duration > 0 ? duration : 100);
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

    private record TotemBody(LivingEntity caster, String lines, SkillContext at) {}

    private final Map<UUID, TotemBody> totemBodies = new ConcurrentHashMap<>();

    private void spawnTotemBody(SkillContext context, SkillContext at, String lines, int duration) {
        Location origin = at.origin();
        ArmorStand body = origin.getWorld().spawn(origin, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setPersistent(false);
        });
        totemBodies.put(body.getUniqueId(), new TotemBody(context.caster(), lines, at));
        Tasks.runLater(plugin, body, duration, () -> {
            totemBodies.remove(body.getUniqueId());
            body.remove();
        });
    }

    @org.bukkit.event.EventHandler(ignoreCancelled = true)
    public void onTotemHit(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        TotemBody totem = totemBodies.get(event.getEntity().getUniqueId());
        if (totem == null) return;
        event.setCancelled(true);
        Entity damager = event.getDamager();
        if (damager instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Entity shooter) damager = shooter;
        if (damager instanceof LivingEntity attacker && !attacker.equals(totem.caster())) {
            executeSteps(inlineSkills.computeIfAbsent(totem.lines(), this::parseInline), 0,
                    new SkillContext(totem.caster(), attacker, null, totem.at().origin(), true));
        }
    }

    private void runTotemLines(String lines, SkillContext context) {
        if (lines != null) executeSteps(inlineSkills.computeIfAbsent(lines, this::parseInline), 0, context);
    }

    private void remove(Target target) {
        if (target.entity() != null) target.entity().remove();
    }

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

            case "modelpart" -> {
                Location bone = betterModel.bonePosition(mobManager.trackerFor(context.caster().getUniqueId()),
                        firstParam(targeterParams, "p", "part", "bone"), context.caster().getLocation());
                yield Target.ofLocation(bone != null ? bone
                        : context.caster().getLocation().add(0, context.caster().getHeight() * 0.6, 0));
            }
            case "pir", "playersinradius" -> nearestPlayer(context.caster(), targeterParams);
            case "self" -> Target.ofEntity(context.caster());

            default -> context.targetIsTrigger() && context.trigger() != null
                    ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster());
        };
    }

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

    private Location forwardLocation(LivingEntity caster, Map<String, String> p) {
        Location origin = Boolean.parseBoolean(p.getOrDefault("uel", "false")) ? caster.getEyeLocation() : caster.getLocation();
        double distance = parseFloat(p.get("f"), 1f);
        double yOffset = parseFloat(p.get("yoffset"), 0f);
        double rotate = parseFloat(p.get("rotate"), 0f);
        Vector direction = origin.getDirection().normalize();

        if (rotate != 0) direction.rotateAroundY(Math.toRadians(-rotate));
        Location target = origin.clone().add(direction.multiply(distance));
        target.add(0, yOffset, 0);
        return target;
    }

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
        public static Target ofEntity(Entity entity) {
            return new Target(entity, null, null);
        }

        public static Target ofBlock(Block block) {
            return new Target(null, block, null);
        }

        public static Target ofLocation(Location location) {
            return new Target(null, null, location);
        }

        public Location location() {
            if (entity != null) return entity.getLocation();
            if (block != null) return block.getLocation().add(0.5, 0.5, 0.5);
            return rawLocation;
        }
    }
}

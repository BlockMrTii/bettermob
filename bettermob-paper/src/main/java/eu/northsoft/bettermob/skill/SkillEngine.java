package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.api.MobInfo;
import eu.northsoft.bettermob.api.custom.CustomMechanic;
import eu.northsoft.bettermob.api.event.skill.BetterMobMechanicEvent;
import eu.northsoft.bettermob.api.event.skill.BetterMobSkillEvent;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.drop.DropRegistry;
import eu.northsoft.bettermob.item.ItemNbt;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.model.ModelRider;
import eu.northsoft.bettermob.skill.condition.ConditionRegistry;
import eu.northsoft.bettermob.skill.mechanic.BuiltinMechanics;
import eu.northsoft.bettermob.skill.mechanic.Mechanic;
import eu.northsoft.bettermob.skill.mechanic.MechanicCall;
import eu.northsoft.bettermob.skill.mechanic.MechanicRegistry;
import eu.northsoft.bettermob.skill.target.TargeterRegistry;
import eu.northsoft.bettermob.stats.SkillStats;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.*;

public final class SkillEngine implements Listener {
    private final BetterMobPlugin plugin;
    private final SkillRegistry registry;
    private final MobManager mobManager;
    private final BetterModelHook betterModel;
    private final ModelEngineHook modelEngine;
    private final ItemRegistry items;
    private final DebugManager debug;
    private final PaperSkillState state = new PaperSkillState();
    private final ModelPassengers modelPassengers = new ModelPassengers();
    private final InlineSkills inlineSkills;
    private final TargeterRegistry targeters;
    private final ConditionRegistry conditionRegistry;
    private final MechanicRegistry mechanics;
    private final CustomPlaceholders placeholders;

    public SkillEngine(BetterMobPlugin plugin, SkillRegistry registry, MobManager mobManager, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.mobManager = mobManager;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
        this.debug = plugin.debug();
        this.inlineSkills = new InlineSkills(line -> plugin.messages().warn("skill.inlineLineInvalid", "line", line));
        this.placeholders = new CustomPlaceholders((namespace, owner, exception) -> plugin.messages().warn("skill.customPlaceholderFailed",
                "namespace", namespace, "plugin", owner == null ? "?" : owner.getName(), "error", exception));
        this.targeters = new TargeterRegistry(this);
        this.conditionRegistry = new ConditionRegistry(this);
        this.mechanics = new MechanicRegistry(plugin);
        BuiltinMechanics.registerAll(mechanics, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(modelPassengers, plugin);
        for (Mechanic mechanic : mechanics.builtins()) {
            if (mechanic instanceof Listener listener) plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    public List<UUID> modelRiders(UUID owner, String seat) {
        List<ModelRider> riders = new ArrayList<>();
        for (UUID id : modelPassengers.riders(owner, seat)) riders.add(new ModelRider(id, seat));
        Object betterTracker = mobManager.trackerFor(owner);
        if (betterTracker != null) riders.addAll(betterModel.riders(betterTracker));
        Object engineTracker = mobManager.modelEngineTrackerFor(owner);
        if (engineTracker != null) riders.addAll(modelEngine.riders(engineTracker));
        return ModelRider.select(riders, seat);
    }

    public UUID modelOwnerOf(UUID rider) {
        UUID owner = modelPassengers.ownerOf(rider);
        if (owner == null) owner = betterModel.ownerOf(rider);
        if (owner == null) owner = modelEngine.ownerOf(rider);
        return owner;
    }

    public ModelPassengers modelPassengers() {
        return modelPassengers;
    }

    public BetterMobPlugin plugin() {
        return plugin;
    }

    public MobManager mobManager() {
        return mobManager;
    }

    public BetterModelHook betterModel() {
        return betterModel;
    }

    public ModelEngineHook modelEngine() {
        return modelEngine;
    }

    private DropRegistry drops;

    public void setDrops(DropRegistry drops) {
        this.drops = drops;
    }

    public DropRegistry drops() {
        return drops;
    }

    public ItemRegistry items() {
        return items;
    }

    public DebugManager debug() {
        return debug;
    }

    public PaperSkillState state() {
        return state;
    }

    public ConditionRegistry conditionRegistry() {
        return conditionRegistry;
    }

    private final ChatPrompts chatPrompts = new ChatPrompts(this);

    public ChatPrompts chatPrompts() {
        return chatPrompts;
    }

    public CustomPlaceholders placeholders() {
        return placeholders;
    }

    public MechanicRegistry mechanics() {
        return mechanics;
    }

    public TargeterRegistry targeters() {
        return targeters;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setVelocity(new Vector());
        Tasks.runLater(plugin, player, 1L, () -> player.setVelocity(new Vector()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        forget(event.getPlayer().getUniqueId());
    }

    public boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic) {
        return mechanics.registerCustom(owner, name, mechanic);
    }

    public void unregisterMechanic(String name) {
        mechanics.unregisterCustom(name);
    }

    public void unregisterMechanics(Plugin owner) {
        mechanics.unregisterCustom(owner);
    }

    public void runById(String skillId, SkillContext context) {
        SkillDefinition skill = registry.get(skillId);
        if (skill == null) {
            plugin.messages().warn("skill.notRegistered", "skill", skillId);
            return;
        }
        run(skill, context);
    }

    public void run(SkillDefinition skill, SkillContext source) {
        Runnable leave = debug.enter(source.caster().getUniqueId());
        try {
            runScoped(skill, source);
        } finally {
            leave.run();
        }
    }

    private void runScoped(SkillDefinition skill, SkillContext source) {
        SkillContext context = source.ownedHere().withArguments(Arguments.withDefaults(source.arguments(), skill.arguments));
        String caster = debug.info() ? subject(context.caster()) : null;
        if (debug.info()) debug.info("skill '" + skill.id + "' started by " + caster, skill.id, caster);
        Check conditions = check(skill.conditions, context, null);
        if (conditions != Check.PASS) {
            if (debug.info()) debug.info("skill '" + skill.id + "' stopped: conditions " + conditions.name().toLowerCase(Locale.ROOT), skill.id, caster);
            return;
        }
        if (!skill.targetConditions.isEmpty()) {
            Target obstructing = needsBlock(skill.targetConditions) ? targeters.resolveAll("obstructingblock", Map.of(), context).get(0) : null;
            Check targetConditions = check(skill.targetConditions, context, obstructing);
            if (targetConditions != Check.PASS) {
                if (debug.info()) debug.info("skill '" + skill.id + "' stopped: target conditions " + targetConditions.name().toLowerCase(Locale.ROOT), skill.id, caster);
                return;
            }
        }
        if (BetterMobSkillEvent.getHandlerList().getRegisteredListeners().length > 0) {
            MobDefinition definition = mobManager.definitionOf(context.caster().getUniqueId());
            BetterMobSkillEvent api = new BetterMobSkillEvent(skill.id, context.caster(), context.trigger(), definition == null ? null : MobInfo.from(definition));
            Bukkit.getPluginManager().callEvent(api);
            if (api.isCancelled()) {
                if (debug.info()) debug.info("skill '" + skill.id + "' stopped: cancelled by a plugin", skill.id, caster);
                return;
            }
        }
        if (skill.cooldown > 0 && !state.acquireSkillCooldown(context.caster(), skill)) {
            if (debug.info()) debug.info("skill '" + skill.id + "' stopped: on cooldown", skill.id, caster);
            return;
        }
        SkillStats stats = plugin.stats();
        if (!stats.enabled()) {
            executeSteps(skill.steps, 0, context);
            return;
        }
        long start = System.nanoTime();
        try {
            executeSteps(skill.steps, 0, context);
        } finally {
            long elapsed = System.nanoTime() - start;
            stats.record(skill.id, elapsed);
            double millis = elapsed / 1e6;
            if (stats.warnMillis() > 0 && millis > stats.warnMillis()) {
                plugin.messages().warn("stats.slowSkill", "skill", skill.id,
                        "millis", String.format(Locale.ROOT, "%.1f", millis), "threshold", stats.warnMillis());
            }
        }
    }

    private static boolean needsBlock(List<String> conditions) {
        for (String raw : conditions) {
            Condition condition = Condition.parse(raw);
            if (condition != null && condition.mentions("blocktype")) return true;
        }
        return false;
    }

    public Location locationParam(String value, SkillContext context) {
        if (value == null || value.isBlank()) return null;
        String text = Params.stripQuotes(value);
        if (text.startsWith("@")) {
            SkillStep.Cursor cursor = new SkillStep.Cursor(text.substring(1));
            String name = cursor.readWord().toLowerCase(Locale.ROOT);
            Map<String, String> params = cursor.peek() == '{' ? SkillStep.parseParams(cursor.readBraced()) : Map.of();
            List<Target> found = targeters.resolveAll(name, substituteTargeter(params, context), context);
            return found.isEmpty() || found.get(0).location() == null ? null : found.get(0).location().clone();
        }
        String[] parts = text.split(",");
        if (parts.length < 3) return null;
        double x = Params.parseNumber(parts[0].trim(), Double.NaN);
        double y = Params.parseNumber(parts[1].trim(), Double.NaN);
        double z = Params.parseNumber(parts[2].trim(), Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) return null;
        World world = parts.length > 3 ? Bukkit.getWorld(parts[3].trim()) : context.caster().getWorld();
        return world == null ? null : new Location(world, x, y, z);
    }

    public String mobType(Entity entity) {
        MobDefinition definition = mobManager.definitionOf(entity.getUniqueId());
        return definition != null ? definition.id : entity.getType().getKey().getKey();
    }

    public String subject(LivingEntity entity) {
        MobDefinition definition = mobManager.definitionOf(entity.getUniqueId());
        return definition != null ? definition.id : entity.getName();
    }

    private enum Check { PASS, FAIL, REDIRECTED }

    private Check check(List<String> conditions, SkillContext context, Target target) {
        for (String raw : conditions) {
            Condition condition = Condition.parse(raw);
            if (condition == null) continue;
            boolean met = conditionRegistry.evaluate(condition, context, target) == (condition.expected() == null || condition.expected());
            if ("castinstead".equals(condition.action())) {
                if (!met) continue;
                if (debug.info()) debug.info("condition '" + condition.name() + "' holds, casting '" + condition.actionValue() + "' instead", condition.actionValue(), subject(context.caster()));
                if (condition.actionValue() != null) runById(condition.actionValue(), context);
                return Check.REDIRECTED;
            }
            if (!met) {
                if (debug.verbose()) debug.verbose("condition '" + condition.name() + "' failed", subject(context.caster()));
                return Check.FAIL;
            }
        }
        return Check.PASS;
    }

    public void runStep(SkillStep step, SkillContext context) {
        executeSteps(List.of(step), 0, context);
    }

    public void runSteps(List<SkillStep> steps, SkillContext context) {
        executeSteps(steps, 0, context);
    }

    private void executeSteps(List<SkillStep> steps, int index, SkillContext source) {
        SkillContext context = source.ownedHere();
        for (int i = index; i < steps.size(); i++) {
            SkillStep step = steps.get(i);
            if (step instanceof SkillStep.Delay delay) {
                int next = i + 1;
                Tasks.runLater(plugin, context.caster(), delay.ticks(), plugin.stats().trackPending(() -> executeSteps(steps, next, context)));
                return;
            }
            if (step instanceof SkillStep.Mechanic mechanic) {
                Deferral deferral = new Deferral();
                if (runMechanic(mechanic, context, deferral)) {
                    if (debug.verbose()) debug.verbose("cancelskill reached", subject(context.caster()));
                    return;
                }
                int next = i + 1;
                if (deferral.await(() -> resumeOnCaster(plugin, context.caster(), () -> executeSteps(steps, next, context)))) return;
            }
        }
    }

    static void resumeOnCaster(Plugin plugin, LivingEntity caster, Runnable rest) {
        Tasks.runOwned(plugin, caster, rest, () -> Tasks.runAt(plugin, caster.getLocation(), rest));
    }

    private boolean runMechanic(SkillStep.Mechanic mechanic, SkillContext context, Deferral deferral) {
        Runnable leave = debug.enter(context.caster().getUniqueId());
        SkillStats stats = plugin.stats();
        long start = stats.enabled() && !mechanic.params().containsKey("delay") ? System.nanoTime() : 0L;
        try {
            return runMechanicScoped(mechanic, context, deferral);
        } finally {
            leave.run();
            if (start != 0L) stats.recordMechanic(mechanic.name(), System.nanoTime() - start);
        }
    }

    private boolean runMechanicScoped(SkillStep.Mechanic mechanic, SkillContext context, Deferral deferral) {
        Map<String, String> p = mechanic.params();

        if (mechanic.inlineCondition() != null) {
            String inline = mechanic.inlineCondition();
            boolean onTrigger = inline.startsWith("~");
            if (onTrigger && context.trigger() == null) {
                if (debug.verbose()) debug.verbose("mechanic '" + mechanic.name() + "' skipped: trigger condition without a trigger", subject(context.caster()));
                return false;
            }
            boolean passes = onTrigger
                    ? conditionPasses(inline.substring(1), context, Target.ofEntity(context.trigger()))
                    : conditionPasses(inline, context, null);
            if (mechanic.negated() == passes) {
                if (debug.verbose()) debug.verbose("mechanic '" + mechanic.name() + "' skipped: inline condition", subject(context.caster()));
                return false;
            }
        }

        if (p.containsKey("delay")) {
            if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;
            int ticks = parseInt(argumentValue(p.get("delay"), context), 0);

            SkillStep.Mechanic withoutDelay = new SkillStep.Mechanic(mechanic.name(),
                    without(without(p, "delay"), "cd"), mechanic.targeter(), mechanic.targeterParams(), null, false);
            Tasks.runLater(plugin, context.caster(), ticks, plugin.stats().trackPending(() -> runMechanic(withoutDelay, context.ownedHere(), new Deferral())));
            return false;
        }

        if (mechanic.name().equals("cancelskill")) return true;

        List<Target> targets = targeters.resolveAll(mechanic.targeter(), substituteTargeter(mechanic.targeterParams(), context), context);

        if (targets.isEmpty()) {
            if (debug.verbose()) debug.verbose("mechanic '" + mechanic.name() + "' skipped: no target for @" + mechanic.targeter(), subject(context.caster()));
            return false;
        }
        if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;
        Map<String, String> params = substitute(mechanic.name(), p, context);
        if (debug.verbose()) {
            debug.verbose("mechanic '" + mechanic.name() + "' @" + (mechanic.targeter().isEmpty() ? "(inherited)" : mechanic.targeter())
                    + " -> " + targets.size() + " target(s), params " + params, subject(context.caster()));
        }
        boolean perTarget = false;
        for (String value : params.values()) {
            if (value.indexOf("<target.var.") >= 0) {
                perTarget = true;
                break;
            }
        }
        for (Target target : targets) dispatch(mechanic, context, target, perTarget ? withTargetVariables(params, target, context) : params, deferral);
        return false;
    }

    private void callSkillByName(SkillStep.Mechanic mechanic, SkillContext context, Target target, Map<String, String> p) {
        boolean forTarget = !mechanic.targeter().isEmpty() && !mechanic.targeter().equals("self") && target.entity() instanceof LivingEntity;
        SkillContext child = forTarget ? context.withTrigger((LivingEntity) target.entity()) : context;
        runById(mechanic.name(), child.withArguments(Arguments.from(p)));
    }

    private void dispatch(SkillStep.Mechanic mechanic, SkillContext context, Target target, Map<String, String> p, Deferral deferral) {
        Mechanic handler = mechanics.get(mechanic.name());
        if (handler == null) {
            if (registry.get(mechanic.name()) != null) callSkillByName(mechanic, context, target, p);
            else plugin.messages().warnOnce("skill.mechanicUnsupported", "mechanic", mechanic.name());
        } else {
            if (BetterMobMechanicEvent.getHandlerList().getRegisteredListeners().length > 0) {
                MobDefinition definition = mobManager.definitionOf(context.caster().getUniqueId());
                BetterMobMechanicEvent api = new BetterMobMechanicEvent(context.caster(), mechanic.name(), java.util.Collections.unmodifiableMap(p), target.entity() != null && Bukkit.isOwnedByCurrentRegion(target.entity()) ? target.entity() : null,
                        definition == null ? null : MobInfo.from(definition));
                Bukkit.getPluginManager().callEvent(api);
                if (api.isCancelled()) return;
            }
            MechanicCall call = new MechanicCall(mechanic, context, target, p, context.caster().getName());
            if (!handler.runsOnTarget()) {
                handler.execute(call);
                return;
            }
            deferral.add();
            Runnable run = () -> {
                try {
                    handler.execute(new MechanicCall(call.step(), call.context().ownedHere(), call.target(), call.params(), call.casterName()));
                } finally {
                    deferral.complete();
                }
            };
            if (target.entity() != null) Tasks.runOwned(plugin, target.entity(), run, deferral::complete);
            else if (target.location() != null) Tasks.runOwnedAt(plugin, target.location(), run);
            else run.run();
        }
    }

    private boolean conditionPasses(String raw, SkillContext context, Target targetOverride) {
        Condition condition = Condition.parse(raw);
        if (condition == null) return true;
        return conditionRegistry.evaluate(condition, context, targetOverride) == (condition.expected() == null || condition.expected());
    }

    private boolean acquireCooldown(SkillContext context, SkillStep.Mechanic mechanic) {
        float seconds = parseFloat(argumentValue(mechanic.params().get("cd"), context), 0f);
        if (seconds <= 0) return true;
        return state.acquireStepCooldown(context.caster(), mechanic, seconds);
    }

    public boolean isApplyingDamage() {
        return state.isApplyingDamage();
    }

    private void runAuraLines(String lines, SkillContext context) {
        if (lines != null) executeSteps(inline(lines), 0, context);
    }

    public void fireAuras(LivingEntity entity, String kind, LivingEntity trigger, Cancellable event) {
        Map<String, Aura> active = state.activeAuras(entity.getUniqueId());
        if (active == null) return;
        for (Aura aura : active.values()) {
            if (!aura.kind.equals(kind) || aura.until <= System.currentTimeMillis()) continue;
            if (aura.cancelEvent && event != null) event.setCancelled(true);
            runAuraLines(aura.onHit, new SkillContext(entity, trigger, event));
        }
    }

    public void forget(UUID entityId) {
        state.forget(entityId);
    }

    public void runInline(String raw, SkillContext context) {
        executeSteps(inline(raw), 0, context);
    }

    public List<SkillStep> inline(String raw) {
        return inlineSkills.get(raw);
    }

    public static List<String> splitInline(String raw) {
        return InlineSkills.splitInline(raw);
    }

    public static String replaceVariables(String value, Map<String, String> skillScope, Map<String, String> casterScope) {
        return Substitutions.replaceVariables(value, skillScope, casterScope);
    }

    public static String replaceVariables(String value, Map<String, String> skillScope, Map<String, String> casterScope, Map<String, String> targetScope) {
        return Substitutions.replaceVariables(value, skillScope, casterScope, targetScope);
    }

    public static String withLocationValues(String value, double x, double y, double z, float yaw, float pitch) {
        return Substitutions.withLocationValues(value, x, y, z, yaw, pitch);
    }

    private Map<String, String> substitute(String mechanicName, Map<String, String> params, SkillContext context) {
        Map<String, String> p = withArguments(withVariables(params, context), context);
        if (!placeholders.isEmpty()) {
            Map<String, String> applied = new LinkedHashMap<>();
            for (Map.Entry<String, String> entry : p.entrySet()) applied.put(entry.getKey(), placeholders.apply(entry.getValue(), context));
            p = applied;
        }
        boolean casterPlaceholders = false;
        for (String value : p.values()) {
            if (value.indexOf("<caster.") >= 0) {
                casterPlaceholders = true;
                break;
            }
        }
        if (!casterPlaceholders) return p;
        var attackDamage = context.caster().getAttribute(Attribute.ATTACK_DAMAGE);
        String damage = String.valueOf(attackDamage == null ? 1.0 : attackDamage.getValue());
        String name = stripSkillSyntax(context.caster().getName());
        boolean escapesItself = mechanicName.equals("command") || mechanicName.equals("message");
        Location at = null;
        for (String value : p.values()) {
            if (value.indexOf("<caster.l.") >= 0) {
                at = context.caster().getLocation();
                break;
            }
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : p.entrySet()) {
            String value = at == null ? entry.getValue() : Substitutions.withLocationValues(entry.getValue(), at.getX(), at.getY(), at.getZ(), at.getYaw(), at.getPitch());
            if (value.indexOf("<caster.mythic_type>") >= 0) value = value.replace("<caster.mythic_type>", mobType(context.caster()));
            if (value.indexOf("<caster.itemnbt.") >= 0) {
                org.bukkit.inventory.ItemStack held = ItemNbt.heldItem(context.caster(), context.event());
                value = Substitutions.withItemNbt(value, key -> ItemNbt.get(held, key));
            }
            result.put(entry.getKey(), Substitutions.withCasterValues(value, damage, name, escapesItself));
        }
        return result;
    }

    private Map<String, String> substituteTargeter(Map<String, String> params, SkillContext context) {
        if (params.isEmpty()) return params;
        String conditions = params.get("conditions");
        Map<String, String> resolved = substitute("", conditions == null ? params : without(params, "conditions"), context);
        if (conditions == null || resolved == params) return resolved;
        Map<String, String> result = new LinkedHashMap<>(resolved);
        result.put("conditions", conditions);
        return result;
    }

    private final Set<String> missingArguments = ConcurrentHashMap.newKeySet();

    public String argumentValue(String value, SkillContext context) {
        if (value == null || value.indexOf("<arg.") < 0) return value;
        return Arguments.resolve(value, context.arguments(), name -> {
            if (missingArguments.add(name)) plugin.messages().warn("skill.argumentMissing", "argument", name);
        });
    }

    private Map<String, String> withArguments(Map<String, String> params, SkillContext context) {
        if (!Arguments.mentions(params)) return params;
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.put(entry.getKey(), argumentValue(entry.getValue(), context));
        }
        return result;
    }

    private Map<String, String> withVariables(Map<String, String> params, SkillContext context) {
        return Substitutions.resolveVariables(params, context.variables(), state.existingVariablesOf(context.caster().getUniqueId()));
    }

    private Map<String, String> withTargetVariables(Map<String, String> params, Target target, SkillContext context) {
        Entity entity = target.entity() != null ? target.entity() : context.trigger();
        Map<String, String> scope = entity == null ? null : state.existingVariablesOf(entity.getUniqueId());
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.put(entry.getKey(), Substitutions.replaceTargetVariables(entry.getValue(), scope));
        }
        return result;
    }
}

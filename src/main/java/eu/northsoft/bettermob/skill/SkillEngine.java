package eu.northsoft.bettermob.skill;

import static eu.northsoft.bettermob.skill.Params.*;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.api.CustomMechanic;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.skill.condition.ConditionRegistry;
import eu.northsoft.bettermob.skill.mechanic.BuiltinMechanics;
import eu.northsoft.bettermob.skill.mechanic.Mechanic;
import eu.northsoft.bettermob.skill.mechanic.MechanicCall;
import eu.northsoft.bettermob.skill.mechanic.MechanicRegistry;
import eu.northsoft.bettermob.skill.target.TargeterRegistry;
import eu.northsoft.bettermob.stats.SkillStats;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillEngine implements org.bukkit.event.Listener {
    private final BetterMobPlugin plugin;
    private final SkillRegistry registry;
    private final MobManager mobManager;
    private final BetterModelHook betterModel;
    private final ModelEngineHook modelEngine;
    private final ItemRegistry items;
    private final DebugManager debug;
    private final SkillState state = new SkillState();
    private final Map<String, List<SkillStep>> inlineSkills = new ConcurrentHashMap<>();
    private final TargeterRegistry targeters;
    private final ConditionRegistry conditionRegistry;
    private final MechanicRegistry mechanics;

    public SkillEngine(BetterMobPlugin plugin, SkillRegistry registry, MobManager mobManager, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.mobManager = mobManager;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
        this.debug = plugin.debug();
        this.targeters = new TargeterRegistry(this);
        this.conditionRegistry = new ConditionRegistry(this);
        this.mechanics = new MechanicRegistry(plugin);
        BuiltinMechanics.registerAll(mechanics, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Mechanic mechanic : mechanics.builtins()) {
            if (mechanic instanceof org.bukkit.event.Listener listener) plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
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

    public ItemRegistry items() {
        return items;
    }

    public DebugManager debug() {
        return debug;
    }

    public SkillState state() {
        return state;
    }

    public TargeterRegistry targeters() {
        return targeters;
    }

    @org.bukkit.event.EventHandler
    public void onRespawn(org.bukkit.event.player.PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        player.setVelocity(new Vector());
        Tasks.runLater(plugin, player, 1L, () -> player.setVelocity(new Vector()));
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

    public void run(SkillDefinition skill, SkillContext context) {
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
            if (condition != null && condition.name().equals("blocktype")) return true;
        }
        return false;
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

    private void executeSteps(List<SkillStep> steps, int index, SkillContext context) {
        for (int i = index; i < steps.size(); i++) {
            SkillStep step = steps.get(i);
            if (step instanceof SkillStep.Delay delay) {
                int next = i + 1;
                Tasks.runLater(plugin, context.caster(), delay.ticks(), plugin.stats().trackPending(() -> executeSteps(steps, next, context)));
                return;
            }
            if (step instanceof SkillStep.Mechanic mechanic && runMechanic(mechanic, context)) {
                if (debug.verbose()) debug.verbose("cancelskill reached", subject(context.caster()));
                return;
            }
        }
    }

    private boolean runMechanic(SkillStep.Mechanic mechanic, SkillContext context) {
        Map<String, String> p = mechanic.params();

        if (mechanic.inlineCondition() != null) {
            boolean passes = conditionPasses(mechanic.inlineCondition(), context, null);
            if (mechanic.negated() == passes) {
                if (debug.verbose()) debug.verbose("mechanic '" + mechanic.name() + "' skipped: inline condition", subject(context.caster()));
                return false;
            }
        }

        if (p.containsKey("delay")) {
            if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;
            int ticks = parseInt(p.get("delay"), 0);

            SkillStep.Mechanic withoutDelay = new SkillStep.Mechanic(mechanic.name(),
                    without(without(p, "delay"), "cd"), mechanic.targeter(), mechanic.targeterParams(), null, false);
            Tasks.runLater(plugin, context.caster(), ticks, plugin.stats().trackPending(() -> runMechanic(withoutDelay, context)));
            return false;
        }

        if (mechanic.name().equals("cancelskill")) return true;

        List<Target> targets = targeters.resolveAll(mechanic.targeter(), mechanic.targeterParams(), context);

        if (targets.isEmpty()) {
            if (debug.verbose()) debug.verbose("mechanic '" + mechanic.name() + "' skipped: no target for @" + mechanic.targeter(), subject(context.caster()));
            return false;
        }
        if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;
        Map<String, String> params = substitute(p, context);
        if (debug.verbose()) {
            debug.verbose("mechanic '" + mechanic.name() + "' @" + (mechanic.targeter().isEmpty() ? "(inherited)" : mechanic.targeter())
                    + " -> " + targets.size() + " target(s), params " + params, subject(context.caster()));
        }
        for (Target target : targets) dispatch(mechanic, context, target, params);
        return false;
    }

    private void dispatch(SkillStep.Mechanic mechanic, SkillContext context, Target target, Map<String, String> p) {
        Mechanic handler = mechanics.get(mechanic.name());
        if (handler == null) plugin.messages().warn("skill.mechanicUnsupported", "mechanic", mechanic.name());
        else {
            MechanicCall call = new MechanicCall(mechanic, context, target, p);
            Runnable run = () -> handler.execute(call);
            if (target.entity() != null) Tasks.runOwned(plugin, target.entity(), run);
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
        float seconds = parseFloat(mechanic.params().get("cd"), 0f);
        if (seconds <= 0) return true;
        return state.acquireStepCooldown(context.caster(), mechanic, seconds);
    }

    public boolean isApplyingDamage() {
        return state.isApplyingDamage();
    }

    private void runAuraLines(String lines, SkillContext context) {
        if (lines != null) executeSteps(inline(lines), 0, context);
    }

    public void fireAuras(LivingEntity entity, String kind, LivingEntity trigger, org.bukkit.event.Cancellable event) {
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
        return inlineSkills.computeIfAbsent(raw, this::parseInline);
    }

    private List<SkillStep> parseInline(String raw) {
        List<SkillStep> steps = new ArrayList<>();
        for (String line : splitInline(raw)) {
            SkillStep step = SkillStep.parse(line);
            if (step == null) plugin.messages().warn("skill.inlineLineInvalid", "line", line);
            else steps.add(step);
        }
        return List.copyOf(steps);
    }

    public static List<String> splitInline(String raw) {
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
        String name = stripSkillSyntax(context.caster().getName());
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : p.entrySet()) {
            result.put(entry.getKey(), entry.getValue()
                    .replace("<caster.damage>", damage).replace("<caster.name>", name));
        }
        return result;
    }
}

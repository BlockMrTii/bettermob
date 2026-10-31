package eu.northsoft.bettermob.skill.core;

import eu.northsoft.bettermob.mob.HealthSpec;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.Params;
import eu.northsoft.bettermob.skill.SkillDefinition;
import eu.northsoft.bettermob.skill.SkillState;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.skill.Substitutions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Platform-neutral skill execution engine for evaluating steps, conditions, targeters, and mechanics.
 */
public class CoreSkillEngine {
    private final SkillState state = new SkillState();
    private final Map<String, CoreSkillCondition> conditions = new ConcurrentHashMap<>();
    private final Map<String, CoreSkillTargeter> targeters = new ConcurrentHashMap<>();
    private final Map<String, CoreSkillMechanic> mechanics = new ConcurrentHashMap<>();
    private Consumer<String> warningHandler = msg -> {};

    public CoreSkillEngine() {
        registerBuiltins();
    }

    public void setWarningHandler(Consumer<String> warningHandler) {
        this.warningHandler = warningHandler != null ? warningHandler : msg -> {};
    }

    public SkillState state() {
        return state;
    }

    public void registerCondition(String name, CoreSkillCondition condition) {
        conditions.put(name.toLowerCase(Locale.ROOT), condition);
    }

    public void registerTargeter(String name, CoreSkillTargeter targeter) {
        targeters.put(name.toLowerCase(Locale.ROOT), targeter);
    }

    public void registerMechanic(String name, CoreSkillMechanic mechanic) {
        mechanics.put(name.toLowerCase(Locale.ROOT), mechanic);
    }

    private void registerBuiltins() {
        registerTargeter("self", (params, ctx) -> List.of(CoreSkillTarget.ofEntity(ctx.caster())));
        registerTargeter("caster", (params, ctx) -> List.of(CoreSkillTarget.ofEntity(ctx.caster())));
        registerTargeter("target", (params, ctx) -> ctx.trigger() != null ? List.of(CoreSkillTarget.ofEntity(ctx.trigger())) : List.of());
        registerTargeter("trigger", (params, ctx) -> ctx.trigger() != null ? List.of(CoreSkillTarget.ofEntity(ctx.trigger())) : List.of());

        registerCondition("chance", (ctx, raw, target) -> {
            float chance = Params.parseFloat(Params.conditionParam(raw, "chance", "c"), 1f);
            return ThreadLocalRandom.current().nextDouble() < chance;
        });

        registerCondition("health", (ctx, raw, target) -> {
            var entity = target != null && target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity l ? l : ctx.caster();
            if (entity == null) return false;
            String text = Params.conditionParam(raw, "health", "h");
            HealthSpec parsed = eu.northsoft.bettermob.mob.MobPhase.parseHealth(text);
            return parsed != null && parsed.matches(entity.health(), entity.maxHealth());
        });

        registerCondition("distance", (ctx, raw, target) -> {
            if (ctx.caster() == null || target == null || target.position() == null) return false;
            Position p1 = ctx.caster().position();
            Position p2 = target.position();
            if (p1 == null || p2 == null || !p1.world().equals(p2.world())) return false;
            double distSq = p1.distanceSquared(p2);
            float req = Params.parseFloat(Params.conditionParam(raw, "distance", "d"), 0f);
            return distSq <= (req * req);
        });

        registerCondition("offgcd", (ctx, raw, target) -> {
            return ctx.caster() == null || !state.hasActiveGcd(ctx.caster().uuid());
        });

        registerCondition("skilloncooldown", (ctx, raw, target) -> {
            if (ctx.caster() == null) return false;
            String skillId = Params.conditionParam(raw, "skill", "s", "name");
            return skillId != null && state.skillOnCooldown(ctx.caster().uuid(), skillId);
        });

        registerCondition("stringequals", (ctx, raw, target) -> {
            Map<String, String> casterVars = ctx.caster() == null ? Map.of() : state.existingVariablesOf(ctx.caster().uuid());
            Map<String, String> targetVars = target != null && target.entity() != null ? state.existingVariablesOf(target.entity().uuid()) : null;
            String v1 = Substitutions.replaceVariables(Params.conditionParam(raw, "val1", "value1", "a"), ctx.variables(), casterVars, targetVars);
            String v2 = Substitutions.replaceVariables(Params.conditionParam(raw, "val2", "value2", "b"), ctx.variables(), casterVars, targetVars);
            return v1.equals(v2);
        });
        registerCondition("stringeq", conditions.get("stringequals"));
    }

    public boolean testCondition(Condition condition, CoreSkillContext context, CoreSkillTarget target) {
        if (condition == null) return true;
        if (condition.composite()) {
            return Condition.holds(condition.params(), atom -> testCondition(atom, context, target))
                    == (condition.expected() == null || condition.expected());
        }
        CoreSkillCondition impl = conditions.get(condition.name().toLowerCase(Locale.ROOT));
        if (impl == null) {
            warningHandler.accept("Unknown skill condition: " + condition.name());
            return true;
        }
        boolean actual = impl.test(context, condition.params(), target);
        boolean expected = condition.expected() == null || condition.expected();
        return actual == expected;
    }

    public void executeStep(SkillStep step, CoreSkillContext context) {
        if (step == null || context == null) return;
        if (!(step instanceof SkillStep.Mechanic mech)) return;

        String targeterName = mech.targeter().isEmpty() ? "self" : mech.targeter().toLowerCase(Locale.ROOT);
        CoreSkillTargeter targeter = targeters.get(targeterName);
        if (targeter == null) {
            warningHandler.accept("Unknown skill targeter: @" + targeterName);
            return;
        }
        List<CoreSkillTarget> resolvedTargets = targeter.resolve(mech.targeterParams(), context);

        CoreSkillMechanic mechanic = mechanics.get(mech.name().toLowerCase(Locale.ROOT));
        if (mechanic == null) {
            warningHandler.accept("Unknown skill mechanic: " + mech.name());
            return;
        }

        for (CoreSkillTarget target : resolvedTargets) {
            if (mech.inlineCondition() != null && !mech.inlineCondition().isBlank()) {
                Condition cond = Condition.parse(mech.inlineCondition());
                if (cond != null) {
                    boolean passes = testCondition(cond, context, target);
                    if (mech.negated()) passes = !passes;
                    if (!passes) continue;
                }
            }

            Map<String, String> casterVars = context.caster() != null ? state.existingVariablesOf(context.caster().uuid()) : null;
            Map<String, String> targetVars = target.entity() != null ? state.existingVariablesOf(target.entity().uuid()) : null;
            Map<String, String> resolvedParams = Substitutions.resolveVariables(mech.params(), context.variables(), casterVars, targetVars);

            mechanic.execute(context, target, resolvedParams);
        }
    }

    public void executeSkill(SkillDefinition skill, CoreSkillContext context) {
        if (skill == null || context == null) return;

        if (skill.cooldown > 0 && context.caster() != null) {
            if (!state.acquireSkillCooldown(context.caster().uuid(), skill)) return;
        }

        for (String raw : skill.conditions) {
            Condition cond = Condition.parse(raw);
            if (cond != null && !testCondition(cond, context, CoreSkillTarget.ofEntity(context.caster()))) {
                return;
            }
        }

        for (SkillStep step : skill.steps) {
            executeStep(step, context);
        }
    }
}

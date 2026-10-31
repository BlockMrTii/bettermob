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

        CoreSkillTargeter inRadius = (params, ctx) -> {
            Position center = ctx.origin() != null ? ctx.origin() : (ctx.caster() != null ? ctx.caster().position() : null);
            if (center == null) return List.of();
            var world = ctx.caster() != null ? ctx.caster().world() : null;
            if (world == null) return List.of();
            double radius = Params.parseNumber(Params.firstParam(params, "r", "radius", "range"), 5.0);
            double radSq = radius * radius;
            boolean playersOnly = "true".equalsIgnoreCase(params.get("playersOnly"));
            var candidates = new java.util.ArrayList<CoreSkillTarget>();
            for (var entity : world.nearbyEntities(center, radius)) {
                if (!entity.isValid()) continue;
                if (playersOnly && !entity.isPlayer()) continue;
                if (ctx.caster() != null && entity.uuid().equals(ctx.caster().uuid())) continue;
                Position p = entity.position();
                if (p != null && center.distanceSquared(p) <= radSq) {
                    candidates.add(CoreSkillTarget.ofEntity(entity));
                }
            }
            String sort = params.getOrDefault("sort", "nearest").toLowerCase(Locale.ROOT);
            switch (sort) {
                case "farthest" -> candidates.sort((a, b) -> Double.compare(center.distanceSquared(b.position()), center.distanceSquared(a.position())));
                case "random" -> java.util.Collections.shuffle(candidates);
                default -> candidates.sort((a, b) -> Double.compare(center.distanceSquared(a.position()), center.distanceSquared(b.position())));
            }
            int limit = Params.parseInt(params.get("limit"), Integer.MAX_VALUE);
            if (candidates.size() > limit) {
                return candidates.subList(0, limit);
            }
            return candidates;
        };

        registerTargeter("entitiesinradius", inRadius);
        registerTargeter("eir", inRadius);

        CoreSkillTargeter playersInRadius = (params, ctx) -> {
            var p = new java.util.HashMap<>(params);
            p.put("playersOnly", "true");
            return inRadius.resolve(p, ctx);
        };
        registerTargeter("playersinradius", playersInRadius);
        registerTargeter("pir", playersInRadius);

        CoreSkillTargeter nearestPlayer = (params, ctx) -> {
            Position center = ctx.origin() != null ? ctx.origin() : (ctx.caster() != null ? ctx.caster().position() : null);
            if (center == null) return List.of();
            var world = ctx.caster() != null ? ctx.caster().world() : null;
            if (world == null) return List.of();
            double radius = Params.parseNumber(Params.firstParam(params, "r", "radius", "range"), 32.0);
            return world.nearestPlayer(center, radius)
                    .map(CoreSkillTarget::ofEntity)
                    .map(List::of)
                    .orElse(List.of());
        };
        registerTargeter("nearestplayer", nearestPlayer);
        registerTargeter("p", nearestPlayer);

        registerMechanic("damage", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                double amount = Params.parseNumber(params.getOrDefault("amount", params.get("a")), 1.0);
                living.damage(amount, ctx.caster());
            }
        });

        registerMechanic("heal", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                double amount = Params.parseNumber(params.getOrDefault("amount", params.get("a")), 1.0);
                living.health(Math.min(living.maxHealth(), living.health() + amount));
            }
        });

        registerMechanic("message", (ctx, target, params) -> {
            String raw = Params.firstParam(params, "m", "message", "msg");
            if (raw == null || target.entity() == null) return;
            String casterName = ctx.caster() != null && ctx.caster().customName() != null ? ctx.caster().customName() : "Unknown";
            String targetName = target.entity().customName() != null ? target.entity().customName() : "Target";
            String rendered = Params.stripQuotes(raw)
                    .replace("<caster.name>", casterName)
                    .replace("<target.name>", targetName);
            target.entity().sendMessage(rendered);
        });
        registerMechanic("msg", (ctx, target, params) -> mechanics.get("message").execute(ctx, target, params));

        registerMechanic("actionbar", (ctx, target, params) -> {
            String raw = Params.firstParam(params, "m", "message", "msg");
            if (raw == null || target.entity() == null) return;
            String casterName = ctx.caster() != null && ctx.caster().customName() != null ? ctx.caster().customName() : "Unknown";
            String targetName = target.entity().customName() != null ? target.entity().customName() : "Target";
            String rendered = Params.stripQuotes(raw)
                    .replace("<caster.name>", casterName)
                    .replace("<target.name>", targetName);
            target.entity().sendActionBar(rendered);
        });

        registerMechanic("sound", (ctx, target, params) -> {
            String name = Params.firstParam(params, "s", "sound");
            if (name == null) return;
            float pitch = Params.parseFloat(params.get("p"), 1f);
            float volume = Params.parseFloat(params.get("v"), 1f);
            Position pos = target.position() != null ? target.position() : (ctx.caster() != null ? ctx.caster().position() : null);
            var world = ctx.caster() != null ? ctx.caster().world() : (target.entity() != null ? target.entity().world() : null);
            if (pos != null && world != null) {
                world.playSound(pos, name, volume, pitch);
            }
        });

        registerMechanic("velocity", (ctx, target, params) -> {
            if (target.entity() == null) return;
            String mode = params.getOrDefault("m", params.getOrDefault("mode", "SET")).trim().toUpperCase(Locale.ROOT);
            eu.northsoft.bettermob.platform.Vec3 change = new eu.northsoft.bettermob.platform.Vec3(
                    Params.parseFloat(params.get("x"), 0f),
                    Params.parseFloat(params.get("y"), 0f),
                    Params.parseFloat(params.get("z"), 0f));
            eu.northsoft.bettermob.platform.Vec3 current = target.entity().velocity();
            if (current == null) current = new eu.northsoft.bettermob.platform.Vec3(0, 0, 0);
            eu.northsoft.bettermob.platform.Vec3 next = switch (mode) {
                case "ADD" -> new eu.northsoft.bettermob.platform.Vec3(current.x() + change.x(), current.y() + change.y(), current.z() + change.z());
                case "MULTIPLY" -> new eu.northsoft.bettermob.platform.Vec3(current.x() * change.x(), current.y() * change.y(), current.z() * change.z());
                case "DIVIDE" -> new eu.northsoft.bettermob.platform.Vec3(
                        change.x() == 0 ? current.x() : current.x() / change.x(),
                        change.y() == 0 ? current.y() : current.y() / change.y(),
                        change.z() == 0 ? current.z() : current.z() / change.z());
                default -> change;
            };
            target.entity().velocity(next);
        });

        registerMechanic("potion", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                String type = Params.firstParam(params, "type", "t");
                if (type == null) type = "speed";
                int duration = Params.parseInt(Params.firstParam(params, "duration", "d"), 20);
                int level = Params.parseInt(Params.firstParam(params, "level", "l"), 1);
                living.addPotionEffect(type, duration, Math.max(0, level - 1));
            }
        });

        registerMechanic("ignite", (ctx, target, params) -> {
            if (target.entity() != null) {
                int ticks = Params.parseInt(Params.firstParam(params, "t", "ticks", "duration", "d"), 100);
                target.entity().setFireTicks(ticks);
            }
        });

        registerMechanic("lightning", (ctx, target, params) -> {
            Position pos = target.position() != null ? target.position() : (ctx.caster() != null ? ctx.caster().position() : null);
            var world = ctx.caster() != null ? ctx.caster().world() : (target.entity() != null ? target.entity().world() : null);
            if (pos != null && world != null) {
                boolean damage = "true".equalsIgnoreCase(params.get("damage"));
                world.strikeLightning(pos, damage);
            }
        });

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

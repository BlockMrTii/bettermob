package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.api.custom.ConditionContext;
import eu.northsoft.bettermob.api.custom.CustomCondition;
import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.Params;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ConditionRegistry {
    private final SkillEngine engine;
    private final Map<String, SkillCondition> conditions = new HashMap<>();

    public ConditionRegistry(SkillEngine engine) {
        this.engine = engine;
        conditions.put("offgcd", new OffGcdCondition(engine));
        conditions.put("onground", new OnGroundCondition());
        conditions.put("hasaura", new HasAuraCondition(engine));
        conditions.put("hastag", new HasTagCondition());
        conditions.put("chance", new ChanceCondition());
        conditions.put("skilloncooldown", new SkillOnCooldownCondition(engine));
        conditions.put("faction", new FactionCondition(engine));
        conditions.put("distance", new DistanceCondition());
        conditions.put("onblock", new OnBlockCondition());
        conditions.put("blocktype", new BlockTypeCondition());
        conditions.put("health", new HealthCondition());
        conditions.put("lineofsight", new LineOfSightCondition());
        conditions.put("los", new LineOfSightCondition());
        conditions.put("world", new WorldCondition());
        conditions.put("biome", new BiomeCondition());
        conditions.put("time", new TimeCondition());
        conditions.put("variable", new VariableCondition(engine));
        conditions.put("varequals", new VariableCondition(engine));
        conditions.put("variableequals", new VariableCondition(engine));
        conditions.put("variableisset", new VariableCondition(engine, VariableCondition.Test.IS_SET));
        conditions.put("variableinrange", new VariableCondition(engine, VariableCondition.Test.IN_RANGE));
        conditions.put("stringeq", new StringEqualsCondition(engine));
        conditions.put("stringequals", new StringEqualsCondition(engine));
        conditions.put("sneaking", new SneakingCondition());
        conditions.put("region", new RegionCondition(engine));
        conditions.put("crouching", new SneakingCondition());
        conditions.put("day", new DayCondition());
        conditions.put("gamemode", new GameModeCondition());
        conditions.put("outside", new OutsideCondition());
        conditions.put("holding", new HoldingCondition(engine));
        conditions.put("moving", new MovingCondition());
        conditions.put("ismoving", new MovingCondition());
        conditions.put("playerwithin", new PlayerWithinCondition());
        conditions.put("playersinradius", new PlayerWithinCondition());
        conditions.put("fieldofview", new FieldOfViewCondition());
        conditions.put("score", new ScoreCondition(engine));
        conditions.put("modelhaspassengers", new ModelRideCondition(engine, ModelRideCondition.Kind.HAS_PASSENGERS));
        conditions.put("ridingmodel", new ModelRideCondition(engine, ModelRideCondition.Kind.RIDING_MODEL));
        conditions.put("mounted", new ModelRideCondition(engine, ModelRideCondition.Kind.MOUNTED));
        conditions.put("owner", new OwnerCondition());
        conditions.put("mythicmobtype", new MobTypeCondition(engine));
        conditions.put("mobtype", new MobTypeCondition(engine));
        conditions.put("boundingboxesoverlap", new BoundingBoxesOverlapCondition());
        conditions.put("killedmob", new KillCondition(engine, false));
        conditions.put("killedmobs", new KillCondition(engine, true));
    }

    private final Map<String, CustomEntry> customs = new ConcurrentHashMap<>();

    private record CustomEntry(Plugin owner, CustomCondition condition) {}

    public boolean has(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return conditions.containsKey(key) || customs.containsKey(key);
    }

    public boolean registerCustom(Plugin owner, String name, CustomCondition condition) {
        String key = name.toLowerCase(Locale.ROOT);
        if (conditions.containsKey(key)) return false;
        return customs.putIfAbsent(key, new CustomEntry(owner, condition)) == null;
    }

    public void unregisterCustom(String name) {
        customs.remove(name.toLowerCase(Locale.ROOT));
    }

    public void unregisterCustom(Plugin owner) {
        customs.values().removeIf(entry -> Objects.equals(entry.owner(), owner));
    }

    public boolean evaluate(Condition condition, SkillContext context, Target targetOverride) {
        if (condition.composite()) {
            return Condition.holds(condition.params(), atom -> evaluate(atom, context, targetOverride) == (atom.expected() == null || atom.expected()));
        }
        SkillCondition found = conditions.get(condition.name());
        if (found == null) {
            CustomEntry custom = customs.get(condition.name());
            if (custom != null) return testCustom(custom, condition, context, targetOverride);

            engine.plugin().messages().warnOnce("skill.conditionUnsupported", "condition", condition.name());
            return true;
        }
        return found.test(context, ConditionParams.resolve(engine, condition.params(), context, targetOverride), targetOverride);
    }

    private boolean testCustom(CustomEntry custom, Condition condition, SkillContext context, Target targetOverride) {
        try {
            return custom.condition().test(new ConditionContext(
                    context.caster(), context.trigger(),
                    targetOverride == null ? null : targetOverride.entity(), targetOverride == null ? null : targetOverride.location(),
                    Params.parsedParams(ConditionParams.resolve(engine, condition.params(), context, targetOverride))));
        } catch (RuntimeException exception) {
            engine.plugin().messages().warn("skill.customConditionFailed", "condition", condition.name(),
                    "plugin", custom.owner() == null ? "?" : custom.owner().getName(), "error", exception);
            return false;
        }
    }
}

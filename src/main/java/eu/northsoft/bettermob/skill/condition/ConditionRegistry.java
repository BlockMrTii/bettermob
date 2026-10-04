package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

import java.util.HashMap;
import java.util.Map;

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
        conditions.put("sneaking", new SneakingCondition());
    }

    private final Map<String, CustomEntry> customs = new java.util.concurrent.ConcurrentHashMap<>();

    private record CustomEntry(org.bukkit.plugin.Plugin owner, eu.northsoft.bettermob.api.CustomCondition condition) {}

    public boolean has(String name) {
        String key = name.toLowerCase(java.util.Locale.ROOT);
        return conditions.containsKey(key) || customs.containsKey(key);
    }

    public boolean registerCustom(org.bukkit.plugin.Plugin owner, String name, eu.northsoft.bettermob.api.CustomCondition condition) {
        String key = name.toLowerCase(java.util.Locale.ROOT);
        if (conditions.containsKey(key)) return false;
        return customs.putIfAbsent(key, new CustomEntry(owner, condition)) == null;
    }

    public void unregisterCustom(String name) {
        customs.remove(name.toLowerCase(java.util.Locale.ROOT));
    }

    public void unregisterCustom(org.bukkit.plugin.Plugin owner) {
        customs.values().removeIf(entry -> java.util.Objects.equals(entry.owner(), owner));
    }

    public boolean evaluate(Condition condition, SkillContext context, Target targetOverride) {
        SkillCondition found = conditions.get(condition.name());
        if (found == null) {
            CustomEntry custom = customs.get(condition.name());
            if (custom != null) return testCustom(custom, condition, context, targetOverride);

            engine.plugin().messages().warn("skill.conditionUnsupported", "condition", condition.name());
            return true;
        }
        return found.test(context, condition.params(), targetOverride);
    }

    private boolean testCustom(CustomEntry custom, Condition condition, SkillContext context, Target targetOverride) {
        try {
            return custom.condition().test(new eu.northsoft.bettermob.api.ConditionContext(
                    context.caster(), context.trigger(),
                    targetOverride == null ? null : targetOverride.entity(), targetOverride == null ? null : targetOverride.location(),
                    eu.northsoft.bettermob.skill.Params.parsedParams(condition.params())));
        } catch (RuntimeException exception) {
            engine.plugin().messages().warn("skill.customConditionFailed", "condition", condition.name(),
                    "plugin", custom.owner() == null ? "?" : custom.owner().getName(), "error", exception);
            return false;
        }
    }
}

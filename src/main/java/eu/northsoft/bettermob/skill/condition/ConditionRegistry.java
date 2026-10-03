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
    }

    public boolean evaluate(Condition condition, SkillContext context, Target targetOverride) {
        SkillCondition found = conditions.get(condition.name());
        if (found == null) {
            engine.plugin().messages().warn("skill.conditionUnsupported", "condition", condition.name());
            return true;
        }
        return found.test(context, condition.params(), targetOverride);
    }
}

package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.Factions;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class FactionCondition implements SkillCondition {
    private final SkillEngine engine;

    public FactionCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return Factions.has(engine.mobManager(), context.caster(), conditionParam(paramsRaw, "faction", "f", "name"));
    }
}

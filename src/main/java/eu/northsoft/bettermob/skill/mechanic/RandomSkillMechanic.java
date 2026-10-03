package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

import java.util.concurrent.ThreadLocalRandom;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class RandomSkillMechanic implements Mechanic {
    private final SkillEngine engine;

    public RandomSkillMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        String list = firstParam(call.params(), "s", "skills", "skill");
        if (list == null) return;
        String[] ids = list.split(",");
        if (ids.length == 0) return;
        engine.runById(ids[ThreadLocalRandom.current().nextInt(ids.length)].trim(), call.context());
    }
}

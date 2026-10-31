package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class RegionCondition implements SkillCondition {
    private final SkillEngine engine;
    private final AtomicBoolean warned = new AtomicBoolean();

    public RegionCondition(SkillEngine engine) {
        this.engine = engine;
    }

    static String[] names(String list) {
        return list.toLowerCase(Locale.ROOT).replace(" ", "").split(",");
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        if (!engine.plugin().regions().available()) {
            if (warned.compareAndSet(false, true)) engine.plugin().messages().warn("worldguard.missing");
            return false;
        }
        String wanted = conditionParam(paramsRaw, "r", "region", "regions");
        if (wanted.isEmpty()) return false;
        Location at = targetOverride != null && targetOverride.location() != null ? targetOverride.location() : context.caster().getLocation();
        Set<String> here = engine.plugin().regions().at(at);
        for (String name : names(wanted)) {
            if (here.contains(name)) return true;
        }
        return false;
    }
}

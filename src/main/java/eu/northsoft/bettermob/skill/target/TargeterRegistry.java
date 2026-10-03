package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TargeterRegistry {
    private final SkillEngine engine;
    private final Map<String, Targeter> targeters = new HashMap<>();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public TargeterRegistry(SkillEngine engine) {
        this.engine = engine;
        CandidateFilters filters = new CandidateFilters(engine);
        register(new EntitiesInRadiusTargeter(filters, false, Integer.MAX_VALUE, true), "entitiesnearorigin", "eno");
        register(new EntitiesInRadiusTargeter(filters, false, Integer.MAX_VALUE, false), "entitiesinradius", "eir", "livingentitiesinradius", "leir");
        register(new EntitiesInRadiusTargeter(filters, true, Integer.MAX_VALUE, false), "playersinradius");
        register(new EntitiesInRadiusTargeter(filters, true, 1, false), "pir");
        register((SingleTargeter) (params, context) -> context.trigger() != null
                ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster()), "trigger", "target");
        register(new ObstructingBlockTargeter(), "obstructingblock");
        register(new ForwardTargeter(), "forward");
        register(new SelfLocationTargeter(), "selflocation");
        register(new ModelPartTargeter(engine), "modelpart");
        register((SingleTargeter) (params, context) -> Target.ofEntity(context.caster()), "self", "caster", "mob");
        register((SingleTargeter) (params, context) -> Target.ofLocation(
                context.origin() != null ? context.origin() : context.caster().getLocation()), "origin");
        register((SingleTargeter) (params, context) -> Target.ofLocation(
                (context.trigger() != null ? context.trigger() : context.caster()).getLocation()), "targetlocation", "tl");
        register(new FixedLocationTargeter(), "location");
        register(new OwnerTargeter(), "owner", "parent");
    }

    private void register(Targeter targeter, String... names) {
        for (String name : names) targeters.put(name, targeter);
    }

    public List<Target> resolveAll(String targeter, Map<String, String> params, SkillContext context) {
        Targeter found = targeters.get(targeter.toLowerCase(Locale.ROOT));
        if (found != null) return found.resolve(params, context);
        if (!targeter.isEmpty()) warnUnknown(targeter);
        return List.of(context.targetIsTrigger() && context.trigger() != null
                ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster()));
    }

    private void warnUnknown(String name) {
        if (warned.add(name)) engine.plugin().messages().warn("skill.targeterUnknown", "targeter", name);
    }
}

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

    private final Map<String, CustomEntry> customs = new java.util.concurrent.ConcurrentHashMap<>();

    private record CustomEntry(org.bukkit.plugin.Plugin owner, eu.northsoft.bettermob.api.CustomTargeter targeter) {}

    public boolean has(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return targeters.containsKey(key) || customs.containsKey(key);
    }

    public boolean registerCustom(org.bukkit.plugin.Plugin owner, String name, eu.northsoft.bettermob.api.CustomTargeter targeter) {
        String key = name.toLowerCase(Locale.ROOT);
        if (targeters.containsKey(key)) return false;
        return customs.putIfAbsent(key, new CustomEntry(owner, targeter)) == null;
    }

    public void unregisterCustom(String name) {
        customs.remove(name.toLowerCase(Locale.ROOT));
    }

    public void unregisterCustom(org.bukkit.plugin.Plugin owner) {
        customs.values().removeIf(entry -> java.util.Objects.equals(entry.owner(), owner));
    }

    private List<Target> resolveCustom(String name, CustomEntry custom, Map<String, String> params, SkillContext context) {
        try {
            List<Target> targets = new java.util.ArrayList<>();
            for (org.bukkit.entity.Entity entity : custom.targeter().resolve(new eu.northsoft.bettermob.api.TargeterContext(
                    context.caster(), context.trigger(), context.origin(), params))) {
                if (entity != null) targets.add(Target.ofEntity(entity));
            }
            return targets;
        } catch (RuntimeException exception) {
            engine.plugin().messages().warn("skill.customTargeterFailed", "targeter", name,
                    "plugin", custom.owner() == null ? "?" : custom.owner().getName(), "error", exception);
            return List.of();
        }
    }

    public List<Target> resolveAll(String targeter, Map<String, String> params, SkillContext context) {
        Targeter found = targeters.get(targeter.toLowerCase(Locale.ROOT));
        if (found != null) return found.resolve(params, context);
        CustomEntry custom = customs.get(targeter.toLowerCase(Locale.ROOT));
        if (custom != null) return resolveCustom(targeter, custom, params, context);
        if (!targeter.isEmpty()) warnUnknown(targeter);
        return List.of(context.targetIsTrigger() && context.trigger() != null
                ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster()));
    }

    private void warnUnknown(String name) {
        if (warned.add(name)) engine.plugin().messages().warn("skill.targeterUnknown", "targeter", name);
    }
}

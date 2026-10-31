package eu.northsoft.bettermob.skill;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger,
                           Map<String, String> variables, Map<String, String> arguments, LivingEntity remoteTrigger) {
    public SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger,
                        Map<String, String> variables, Map<String, String> arguments) {
        this(caster, trigger, event, origin, targetIsTrigger, variables, arguments, null);
    }

    public SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger,
                        Map<String, String> variables) {
        this(caster, trigger, event, origin, targetIsTrigger, variables, Map.of());
    }

    public SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger) {
        this(caster, trigger, event, origin, targetIsTrigger, new ConcurrentHashMap<>());
    }

    public SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event) {
        this(caster, trigger, event, null, false);
    }

    public static SkillContext of(LivingEntity caster) {
        return new SkillContext(caster, null, null);
    }

    public SkillContext withTrigger(LivingEntity newTrigger) {
        return new SkillContext(caster, newTrigger, event, origin, true, variables, arguments);
    }

    public SkillContext ownedHere() {
        LivingEntity known = trigger != null ? trigger : remoteTrigger;
        if (known == null) return this;
        boolean owned = Bukkit.isOwnedByCurrentRegion(known);
        if (owned == (trigger != null)) return this;
        return new SkillContext(caster, owned ? known : null, event, origin, targetIsTrigger, variables, arguments, owned ? null : known);
    }

    public SkillContext withOrigin(Location newOrigin) {
        return new SkillContext(caster, trigger, event, newOrigin, targetIsTrigger, variables, arguments, remoteTrigger);
    }

    public SkillContext withArguments(Map<String, String> newArguments) {
        return new SkillContext(caster, trigger, event, origin, targetIsTrigger, variables, newArguments, remoteTrigger);
    }
}

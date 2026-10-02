package eu.northsoft.bettermob.skill;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

public record SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger) {
    public SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event) {
        this(caster, trigger, event, null, false);
    }

    public static SkillContext of(LivingEntity caster) {
        return new SkillContext(caster, null, null);
    }

    public SkillContext withTrigger(LivingEntity newTrigger) {
        return new SkillContext(caster, newTrigger, event, origin, true);
    }

    public SkillContext withOrigin(Location newOrigin) {
        return new SkillContext(caster, trigger, event, newOrigin, targetIsTrigger);
    }
}

package eu.northsoft.bettermob;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

record SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger) {
    SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event) {
        this(caster, trigger, event, null, false);
    }

    static SkillContext of(LivingEntity caster) {
        return new SkillContext(caster, null, null);
    }

    SkillContext withTrigger(LivingEntity newTrigger) {
        return new SkillContext(caster, newTrigger, event, origin, true);
    }

    SkillContext withOrigin(Location newOrigin) {
        return new SkillContext(caster, trigger, event, newOrigin, targetIsTrigger);
    }
}
